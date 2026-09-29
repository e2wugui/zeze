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
	private static ZokerManagerConf conf;
	private LogAgent logAgent;

	public static LogAgentManager getInstance() {
		return logAgentManager;
	}

	public static void init(String configXml) throws Exception {
		logAgentManager = new LogAgentManager();
		var config = Config.load(configXml);
		conf = new ZokerManagerConf();
		config.parseCustomize(conf);
		ApiToken.configure(conf.token);
		BrowserOriginGuard.configure(conf.bind);
		logAgentManager.logAgent = new LogAgent(config);
		logAgentManager.logAgent.start();
		startHttpServer();
	}

	public LogAgent getLogAgent() {
		return logAgent;
	}

	private static void startHttpServer() throws Exception {
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
		httpServer.start(netty, conf.bind, 9980);
	}

	// 四个JSON API的实际body仅几KB：无界上限（Integer.MAX_VALUE）下单个大请求体即整体
	// 入堆（OOM/长GC），拖垮整个进程（含LogService）。上限与token门（ApiToken）互相独立：
	// 认证不收敛请求体大小。
	private static final int API_MAX_CONTENT_LENGTH = 1024 * 1024;

	private static void addHandler(String path, HttpEndStreamHandle handle) {
		httpServer.addHandler(path, API_MAX_CONTENT_LENGTH, TransactionLevel.Serializable, DispatchMode.Normal, handle);
	}
}
