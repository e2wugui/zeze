package Zeze.Services.Log4jQuery;

import harness.Extra;
import harness.Fast;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
@Extra
public class TestLog4jQ {
	// 每用例独立 logDir：Log4jFileManager 构造期对同 logDir 独占登记（log4j-02），
	// 本类旧形态两用例共用缺省目录且不 stop——正是登记表按设计暴露的泄漏形态。
	@TempDir
	Path logDir;

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testSearch() throws Exception {
		var beginDate = Calendar.getInstance();
		beginDate.add(Calendar.MINUTE, -30);
		var beginTime = beginDate.getTime().getTime();
		var endTime = -1; // Log4jLog.parseTime("23-08-25 09:19:00.816");
		var logActive = "zeze.log";
		var pattern = "ShutdownHook: ShutdownHook end";
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = logActive;
		logConf.logDir = logDir.toString();
		var logManager = new Log4jFileManager(logConf);
		try {
			var session = new Log4jSession(logManager);
			var result = new ArrayList<Log4jLog>();
			//var reset = false; // reset会导致搜索全部日志，可能很慢，先不测试reset了。
			while (session.searchContains(result, beginTime, endTime, java.util.List.of(pattern), BCondition.ContainsAll, 1)) {
				System.out.println("------------------------");
				for (var log : result)
					System.out.println(log);
			}
			if (!result.isEmpty()) {
				System.out.println("------------------------");
				for (var log : result)
					System.out.println(log);
			}
		} finally {
			logManager.stop();
		}
	}

	@Test
	public void testBrowse() throws Exception {
		var beginDate = Calendar.getInstance();
		beginDate.add(Calendar.MINUTE, -10);
		var beginTime = beginDate.getTime().getTime();
		var endTime = -1; // Log4jLog.parseTime("23-08-25 09:19:01.239");
		var logActive = "zeze.log";
		var pattern = "ShutdownHook: ShutdownHook end";
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = logActive;
		logConf.logDir = logDir.toString();

		var logManager = new Log4jFileManager(logConf);
		try {
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
		} finally {
			logManager.stop();
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
