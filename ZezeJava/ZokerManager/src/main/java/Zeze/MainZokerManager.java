package Zeze;

import Zeze.Config;
import Zeze.Services.LogService;
import Zeze.Util.Task;
import Zeze.log.LogAgentManager;
import Zeze.log.ZokerManagerConf;

/**
 * ZokerManager 进程入口：启动 LogService 与日志查询代理（含 HTTP 服务）。
 */
public class MainZokerManager {
	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();

		var configXml = "server.xml";
		// 部署契约（FND29 zokermanager-02）先于一切服务启动校验：非回环 Bind 且未配 Token
		// 直接抛错退出，不启动 LogService/LogAgent——避免 fail-fast 后残留半启动线程。
		var conf = new ZokerManagerConf();
		Config.load(configXml).parseCustomize(conf);
		ZokerManagerConf.checkDeployPolicy(conf.bind, conf.token);

		var logService = new LogService(Config.load(configXml));
		logService.start();
		LogAgentManager.init(configXml);
	}
}
