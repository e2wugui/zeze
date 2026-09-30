package Zeze.log;

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
	private static LogAgentManager logAgentManager;
	public static HttpServer httpServer;
	// 管理口 event loop 线程组：进程形态由进程生命周期持有；stop（启动失败清理）关闭。
	private static Netty adminNetty;
	private static ZokerManagerConf conf;
	private LogAgent logAgent;

	public static LogAgentManager getInstance() {
		return logAgentManager;
	}

	public static void init(String configXml) throws Exception {
		// 重入守卫：未先 stop 的二次 init 会先覆盖静态引用再走启动序列——bind"成功"
		// （SO_REUSEADDR/REUSEPORT 同口双绑）时双 HttpServer 分流、第一实例组件全部
		// 失联泄漏；bind 失败时收尾 stop() 按静态字段回收，误关第一实例的 adminNetty、
		// 停第二实例的 agent，第一 LogAgent 永久泄漏。已初始化即拒（logAgentManager
		// 非 null ⇔ 已初始化：成功即置位、失败收尾 stop() 复位），重启语义由调用方
		// 显式 stop 后再 init 承担。
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
			// 调用方（MainZokerManager.start 统一收尾）。
			stop();
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
		// 会话绑定表是 FileSessionManager 的类级静态，不随 agent 生命周期走：stop 不清
		// 时嵌入宿主 stop→init 后同 IP 复用持有已停 agent 的死会话（恒 system error 且
		// 闲置清扫被复用前的活跃刷新挡住）。断流量入口后、停 agent 前清理（CloseSession
		// RPC 仍可经 agent 连接发出，异步不挡停机）；纯绑定形态（logAgentManager 为
		// null）同样清理。
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
		httpServer.addFileHandler("/", "web");
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

	private static void addHandler(String path, HttpEndStreamHandle handle) {
		httpServer.addHandler(path, API_MAX_CONTENT_LENGTH, TransactionLevel.Serializable, DispatchMode.Normal, handle);
	}
}
