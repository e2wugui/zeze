package TestLog4jQuery;

import java.util.ArrayList;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Config;
import Zeze.Services.LogAgent;
import Zeze.Services.LogService;
import Zeze.Util.Task;
import org.junit.Test;

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

			Thread.sleep(1000); // 等待ServiceManager订阅成功。

			System.out.println("----------------------------");

			var cond = new BCondition.Data();
			cond.setBeginTime(-1);
			cond.setEndTime(-1);
			cond.setContainsType(BCondition.ContainsAll);
			var words = new ArrayList<String>();
			words.add("23-08-25 09:19:00.813");
			cond.setWords(words);
			for (var serverName : logAgent.getLogServers()) {
				System.out.println("search --------->" + serverName);
				try (var session = logAgent.newSession(serverName)) {
					// 这个session可以保存到http-session中，重复使用时，从上一次的位置继续搜索，
					// 下面的reset参数控制从头开始。
					var rData = session.search(3, false, cond).get();
					System.out.println(rData);
				}
			}
			try (var sessionAll = logAgent.newSessionAll()) {
				var rData = sessionAll.search(3, false, cond);
				System.out.println(rData);
			}
		} finally {
			logAgent.stop();
			logService.stop();
		}
	}
}
