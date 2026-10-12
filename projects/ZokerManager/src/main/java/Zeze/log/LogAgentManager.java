package Zeze.log;

import java.io.File;
import Zeze.Config;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.LogAgent;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.log.handle.BrowseLogHandle;
import Zeze.log.handle.GetLogServersHandle;
import Zeze.log.handle.QueryHandle;
import Zeze.log.handle.SearchLogHandle;

/**
 * 日志查询代理管理：持有 LogAgent，并注册查询 HTTP 服务的各 API 路由与静态页面。
 */
public class LogAgentManager {
	// 静态资源根：相对路径随进程 CWD 解析（与 server.xml 同为工作目录相对——部署以本目录为 CWD 启动）。
	private static final String WEB_ROOT = "web";

	private static LogAgentManager logAgentManager;
	// 包内可见（非 public）：同包直测（TestAdminPortBindFailureFailsFast 等）注入/复位此静态引用。
	static HttpServer httpServer;
	// 管理口 event loop 线程组：进程形态由进程生命周期持有；stop（启动失败清理）关闭。
	private static Netty adminNetty;
	private static ZokerManagerConf conf;
	private LogAgent logAgent;

	public static LogAgentManager getInstance() {
		return logAgentManager;
	}

	public static void init(String configXml) throws Exception {
		// 重入守卫：未先 stop 的二次 init 会先覆盖静态引用——bind"成功"（同口双绑）时双
		// HttpServer 分流、第一实例失联泄漏；bind 失败时收尾 stop() 按静态字段误关第一
		// 实例组件。已初始化即拒（logAgentManager 非 null ⇔ 已初始化：成功即置位、失败
		// 收尾 stop() 复位），重启语义由调用方显式 stop 后再 init 承担。
		if (logAgentManager != null)
			throw new IllegalStateException("LogAgentManager already initialized; call stop() before re-init");
		logAgentManager = new LogAgentManager();
		try {
			var config = Config.load(configXml);
			conf = new ZokerManagerConf();
			config.parseCustomize(conf);
			ApiToken.configure(conf.token);
			BrowserOriginGuard.configure(conf.bind);
			logAgentManager.logAgent = new LogAgent(config);
			logAgentManager.logAgent.start();
			adminNetty = startHttpServer();
		} catch (Throwable e) {
			// 半启动回收：失败时回收本层已启动组件并复位静态引用，不把半启动状态留给
			// 调用方（MainZokerManager.start 统一收尾）。回收失败不掩盖原始启动异常
			// （对齐 MainZokerManager.start 的收尾纪律）：回收异常以 suppressed 附着。
			try {
				stop();
			} catch (Throwable stopEx) {
				e.addSuppressed(stopEx);
			}
			throw e;
		}
	}

	/**
	 * 停止查询代理与管理口 HTTP 服务（启动失败清理与停机共用）：幂等，按启动逆序
	 * 回收（先关管理口断流量入口，再清会话绑定表，最后停 agent），复位静态引用。
	 */
	public static void stop() throws Exception {
		if (httpServer != null) {
			httpServer.close();
			httpServer = null;
		}
		if (adminNetty != null) {
			adminNetty.close();
			adminNetty = null;
		}
		// 会话绑定表是 FileSessionManager 的类级静态，不随 agent 生命周期走，不清理时嵌入
		// 宿主 stop→init 后同 IP 复用持有已停 agent 的死会话（见 closeAllBindings）。
		// 断流量入口后、停 agent 前清理（CloseSession RPC 仍经 agent 连接发出，异步不挡
		// 停机）；纯绑定形态（logAgentManager 为 null）同样清理。
		FileSessionManager.closeAllBindings();
		if (logAgentManager != null) {
			if (logAgentManager.logAgent != null)
				logAgentManager.logAgent.stop();
			logAgentManager = null;
		}
	}

	public LogAgent getLogAgent() {
		return logAgent;
	}

	private static Netty startHttpServer() throws Exception {
		return startAdminHttpServer(conf, 9980);
	}

	/**
	 * 起管理口 HTTP 服务并<b>同步确认 bind 结果</b>（9980 固定端口由 startHttpServer
	 * 收口；port 参数供直测注入）：HttpServer.start 不同步 bind 的 ChannelFuture、失败
	 * 无日志（异步发生在 event loop 上），必须同步确认——否则 bind 失败（端口占用/坏
	 * 地址）呈零可观测的"健康"僵尸（非守护 event loop 线程使进程存活但 9980 无人监听）。
	 * 失败时回收 event loop 线程组与半启动 server 后抛出含 cause 的异常。
	 *
	 * @return Netty 事件循环组持有者（进程形态由进程生命周期持有；直测形态调用方关闭）。
	 */
	static Netty startAdminHttpServer(ZokerManagerConf conf, int port) throws Exception {
		// 部署契约（FND29 zokermanager-02）：默认绑回环；绑非回环必须同时配置 Token，
		// 未配则此处 fail-fast，杜绝"无认证的全集群日志读取面绑上网络"的组合。
		ZokerManagerConf.checkDeployPolicy(conf.bind, conf.token);

		httpServer = new HttpServer();
		var netty = new Netty();

		addHandler("/api/get_log_servers", new GetLogServersHandle());
		addHandler("/api/browse", new BrowseLogHandle());
		addHandler("/api/search", new SearchLogHandle());
		addHandler("/api/query", new QueryHandle());
		// 根路径直出 index.html：文件前缀 handler 对空 subPath 命中的是目录（canListPath=false）→
		// 404，浏览器地址栏裸 "/" 将无页面。精确 handler 优先于前缀匹配；sendFile 全路径自收尾
		// （缺文件回 404，同文件服务语义）；与下方文件服务同根（WEB_ROOT），零拷贝非阻塞可 Direct。
		httpServer.addHandler("/", 0, TransactionLevel.None, DispatchMode.Direct,
				x -> x.sendFile(new File(WEB_ROOT, "index.html")));
		httpServer.addFileHandler("/", WEB_ROOT);
		var future = httpServer.start(netty, conf.bind, port);
		future.awaitUninterruptibly();
		if (!future.isSuccess()) {
			httpServer.close();
			netty.close();
			httpServer = null;
			throw new IllegalStateException("HTTP admin port bind failed on " + conf.bind + ":" + port
					+ " - check port conflict or bind address.",
					future.cause());
		}
		return netty;
	}

	// 四个JSON API的实际body仅几KB：无界上限（Integer.MAX_VALUE）下单个大请求体即整体
	// 入堆（OOM/长GC），拖垮整个进程（含LogService）。上限与token门（ApiToken）互相独立：
	// 认证不收敛请求体大小。
	private static final int API_MAX_CONTENT_LENGTH = 1024 * 1024;

	// 事务级别显式 None：本 HttpServer 无 Application（noProcedure=true），任何事务级别都不会
	// 产生事务包裹——写 Serializable 是无效配置且误导读者以为有事务语义。Normal 派发必须保留：
	// 处理器含 60s 级 RPC 等待，不得占 EventLoop。
	private static void addHandler(String path, HttpEndStreamHandle handle) {
		httpServer.addHandler(path, API_MAX_CONTENT_LENGTH, TransactionLevel.None, DispatchMode.Normal, handle);
	}
}
