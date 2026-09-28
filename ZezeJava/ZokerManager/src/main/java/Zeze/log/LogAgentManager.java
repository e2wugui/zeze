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
	private LogAgent logAgent;

	public static LogAgentManager getInstance() {
		return logAgentManager;
	}

	public static void init(String configXml) throws Exception {
		logAgentManager = new LogAgentManager();
		logAgentManager.logAgent = new LogAgent(Config.load(configXml));
		logAgentManager.logAgent.start();
		startHttpServer();
	}

	public LogAgent getLogAgent() {
		return logAgent;
	}

	private static void startHttpServer() throws Exception {
		httpServer = new HttpServer();
		var netty = new Netty();

		addHandler("/api/get_log_servers", new GetLogServersHandle());
		addHandler("/api/browse", new BrowseLogHandle());
		addHandler("/api/search", new SearchLogHandle());
		addHandler("/api/query", new QueryHandle());
		httpServer.addFileHandler("/", "web");
		httpServer.start(netty, 9980);
	}

	// 四个JSON API的实际body仅几KB：无界上限（Integer.MAX_VALUE）下未认证端口的单个
	// 大请求体即整体入堆（OOM/长GC），拖垮整个进程（含LogService）。
	private static final int API_MAX_CONTENT_LENGTH = 1024 * 1024;

	private static void addHandler(String path, HttpEndStreamHandle handle) {
		httpServer.addHandler(path, API_MAX_CONTENT_LENGTH, TransactionLevel.Serializable, DispatchMode.Normal, handle);
	}
}
