package TestLog4jQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import Log4jQuery.Log4jLog;
import Log4jQuery.Log4jSession;
import org.junit.Test;

public class TestLog4jQuery {
	@Test
	public void testSearch() throws IOException {
		var beginTime = Log4jLog.parseTime("23-08-25 09:19:00.801");
		var endTime = Log4jLog.parseTime("23-08-25 09:19:00.816");
		var logActive = "log/zeze.log";
		var pattern = "23-08-25 09:19:00.813";
		var session = new Log4jSession(logActive, null, beginTime, endTime, java.util.List.of(pattern));
		var result = new ArrayList<Log4jLog>();
		while (session.search(result, 1)) {
			System.out.println("------------------------");
			for (var log : result)
				System.out.println(log);
		}
		if (!result.isEmpty()) {
			System.out.println("------------------------");
			for (var log : result)
				System.out.println(log);
		}
	}

	@Test
	public void testBrowse() throws IOException {
		var beginTime = Log4jLog.parseTime("23-08-25 09:19:00.795");
		var endTime = Log4jLog.parseTime("23-08-25 09:19:01.239");
		var logActive = "log/zeze.log";
		var pattern = "ZezeTaskPool-101";
		var session = new Log4jSession(logActive, null, beginTime, endTime, java.util.List.of(pattern));
		var result = new LinkedList<Log4jLog>();
		while (session.browse(result, 3, 0.4f)) {
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
}
