package TestLog4jQuery;

import java.util.ArrayList;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Config;
import Zeze.Services.LogAgent;
import Zeze.Services.LogService;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;

public class TestLogService {
	@Test
	public void testLogService() throws Exception {
		Task.tryInitThreadPool();

		var configXml = "server.xml";
		var logService = new LogService(Config.load(configXml));
		var logAgent = new LogAgent(Config.load(configXml));

		try {
			logService.start();
			logAgent.start();

			// 等待 ServiceManager 订阅推送到达：LogService 注册后 logAgent 的 client 才会出现 connector，
			// 否则下面 getLogServers() 为空、循环静默跳过。LogAgent 不是 Application，且注册 identity 不是纯
			// serverId（LogServiceConf.formatServiceIdentity 拼了 ip_port），不能复用 TestEnv.waitServerRegistered，
			// 按同样的 100ms 间隔、60s 兜底轮询。
			var deadline = System.currentTimeMillis() + 60_000;
			while (logAgent.getLogServers().isEmpty()) {
				if (System.currentTimeMillis() > deadline)
					throw new IllegalStateException("等待 Zeze.LogService 注册推送超时(60s)");
				Thread.sleep(100);
			}

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
