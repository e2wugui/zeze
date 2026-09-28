package Zeze;

import Zeze.Services.LogService;
import Zeze.Util.Task;
import Zeze.log.LogAgentManager;

/**
 * ZokerManager 进程入口：启动 LogService 与日志查询代理（含 HTTP 服务）。
 */
public class MainZokerManager {
	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();

		var configXml = "server.xml";
		var logService = new LogService(Config.load(configXml));
		logService.start();
		LogAgentManager.init(configXml);
	}
}
