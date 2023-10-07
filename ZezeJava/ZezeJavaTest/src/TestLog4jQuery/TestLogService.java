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
		Task.tryInitThreadPool(null, null, null);

		var configXml = "server.xml";
		var logService = new LogService(Config.load(configXml));
		var logAgent = new LogAgent(Config.load(configXml));

		try {
			logService.start();
			logAgent.start();

			Thread.sleep(2000);
			System.out.println("----------------------------");

			var cond = new BCondition.Data();
			cond.setBeginTime(-1);
			cond.setEndTime(-1);
			cond.setContainsAll(true);
			var words = new ArrayList<String>();
			words.add("23-08-25 09:19:00.813");
			cond.setWords(words);
			for (var serverName : logAgent.getLogServers()) {
				System.out.println("search --------->" + serverName);
				try (var session = logAgent.newSession(serverName)) {
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
