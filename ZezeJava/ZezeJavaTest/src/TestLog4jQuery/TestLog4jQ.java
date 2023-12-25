package TestLog4jQuery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;
import org.junit.Before;
import org.junit.Test;

public class TestLog4jQ {
	@Before
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testSearch() throws Exception {
		var beginDate = Calendar.getInstance();
		beginDate.add(Calendar.DAY_OF_MONTH, -1);
		var beginTime = beginDate.getTime().getTime();
		var endTime = -1; // Log4jLog.parseTime("23-08-25 09:19:00.816");
		var logActive = "zeze.log";
		var pattern = "23-10-16 17:01:59.497";
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = logActive;
		var logManager = new Log4jFileManager(logConf);
		var session = new Log4jSession(logManager);
		var result = new ArrayList<Log4jLog>();
		var reset = false;
		while (session.searchContains(result, beginTime, endTime, java.util.List.of(pattern), BCondition.ContainsAll, 1)) {
			System.out.println("------------------------");
			for (var log : result)
				System.out.println(log);
			if (!reset) {
				session.reset();
				reset = true;
				System.out.println("-------------reset-----------");
			}
		}
		if (!result.isEmpty()) {
			System.out.println("------------------------");
			for (var log : result)
				System.out.println(log);
		}
	}

	@Test
	public void testBrowse() throws Exception {
		var beginDate = Calendar.getInstance();
		beginDate.add(Calendar.DAY_OF_MONTH, -1);
		var beginTime = beginDate.getTime().getTime();
		var endTime = -1; // Log4jLog.parseTime("23-08-25 09:19:01.239");
		var logActive = "zeze.log";
		var pattern = "ZezeTaskPool-101";
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = logActive;

		var logManager = new Log4jFileManager(logConf);
		var session = new Log4jSession(logManager);
		var result = new LinkedList<Log4jLog>();
		while (session.browseContains(result, beginTime, endTime,
				java.util.List.of(pattern), BCondition.ContainsAll, 3, 0.4f)) {
			System.out.println("++++++++++++++++++++++");
			for (var log : result)
				System.out.println(log);
		}
		if (!result.isEmpty()) {
			System.out.println("++++++++++++++++++++++");
			for (var log : result)
				System.out.println(log);
		}
	}

	public static void main(String [] args) throws Exception {
		Task.tryInitThreadPool();
		var test = new TestLog4jQ();
		for (var i = 0; i < 10; ++i) {
			test.testSearch();
			Thread.sleep(60_000);
		}
	}
}
