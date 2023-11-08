package Zeze;

import Zeze.Services.LogService;
import Zeze.Util.Task;
import Zeze.log.LogAgentManager;

public class MainZokerManager {
	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();

		var configXml = "server.xml";
		var logService = new LogService(Config.load(configXml));
		logService.start();
		LogAgentManager.init(configXml);
	}
}
