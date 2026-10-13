package Zeze.Services.Log4jQuery;

import harness.Extra;
import java.util.ArrayList;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Config;
import Zeze.Services.LogAgent;
import Zeze.Services.LogService;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;

@Extra
public class TestLogService {
	@Test
	public void testLogService() throws Exception {
		Task.tryInitThreadPool();

		var configXml = "server.xml";
		// 自包含改造（2026-10-13 随迁）：原等待真实 ServiceManager 订阅推送（依赖 extraTest 车道的
		// 进程内 SM 环境），ZokerManager 自有构建无该环境。两侧 disable SM（createServiceManager
		// 返回 null，start 的 SM 分支跳过）后手工 applyOnChanged 模拟推送（对齐
		// TestLog4jAllViewNoReachableServer 的自包含接线）；地址取 server.xml 中
		// Zeze.LogService.Server acceptor 的实际配置，不硬编码。
		var serverConf = Config.load(configXml);
		serverConf.setServiceManager("disable");
		var agentConf = Config.load(configXml);
		agentConf.setServiceManager("disable");
		var logService = new LogService(serverConf);
		var logAgent = new LogAgent(agentConf);

		final var ip = new String[]{"127.0.0.1"};
		final var port = new int[1];
		serverConf.getServiceConf("Zeze.LogService.Server").forEachAcceptor2(a -> {
			if (!a.getIp().isBlank())
				ip[0] = a.getIp();
			port[0] = a.getPort();
			return true;
		});

		try {
			logService.start();
			logAgent.start();

			// 手工模拟 SM 推送（原为轮询等待真实订阅）：identity 仅影响显示，connector 按 ip/port 建。
			var edit = new BEditService();
			edit.getAdd().add(new BServiceInfo("Zeze.LogService", "@" + ip[0] + ":" + port[0], 0, ip[0], port[0]));
			logAgent.applyOnChanged(edit);
			if (logAgent.getLogServers().isEmpty())
				throw new IllegalStateException("手工推送注册后 getLogServers 仍为空");

			System.out.println("----------------------------");

			var cond = new BCondition.Data();
			cond.setBeginTime(-1);
			cond.setEndTime(-1);
			cond.setContainsType(BCondition.ContainsAll);
			var words = new ArrayList<String>();
			words.add("ShutdownHook: ShutdownHook end");
			cond.setWords(words);
			var logName = "zeze.log";
			for (var serverName : logAgent.getLogServers()) {
				System.out.println("search --------->" + serverName);
				try (var session = logAgent.newSession(serverName, logName)) {
					// 这个session可以保存到http-session中，重复使用时，从上一次的位置继续搜索，
					// 下面的reset参数控制从头开始。
					var rData = session.search(3, false, cond).get();
					System.out.println(rData);
				}
			}
			try (var sessionAll = logAgent.newSessionAll("zeze.log")) {
				var rData = sessionAll.search(3, false, cond);
				System.out.println(rData);
			}
		} finally {
			logAgent.stop();
			logService.stop();
		}
	}
}
