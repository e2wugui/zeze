package Zeze.Services.Log4jQuery;

import harness.Extra;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static harness.DirCleanup.deleteBestEffort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** A sampled index end is not the last timestamp of a rotated log file. */
@Fast
@Extra
public class TestRotationSampleGapKeepsTail {
	private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 28, 12, 0);
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");

	@Test
	public void searchContainsFindsUnindexedTailBeforeClockRollback() throws Exception {
		checkTail(0);
	}

	@Test
	public void searchRegexFindsUnindexedTailBeforeClockRollback() throws Exception {
		checkTail(1);
	}

	@Test
	public void browseContainsFindsUnindexedTailBeforeClockRollback() throws Exception {
		checkTail(2);
	}

	@Test
	public void browseRegexFindsUnindexedTailBeforeClockRollback() throws Exception {
		checkTail(3);
	}

	private static void checkTail(int operation) throws Exception {
		Task.tryInitThreadPool();
		var directory = Files.createTempDirectory("log-rotation-sample-gap");
		Log4jFileManager manager = null;
		try {
			AtomicFileWriter.replace(directory.resolve("zeze.log"), line(0, "head").getBytes(StandardCharsets.UTF_8));
			var conf = new LogServiceConf.LogConf();
			conf.logActive = "zeze.log";
			conf.logDir = directory.toString();
			manager = new Log4jFileManager(conf);
			manager.stop(); // Deliver the real rotation callback explicitly, without timing dependencies.
			// Appending inside the ten-second sampling interval cannot advance index.endTime.
			Files.writeString(directory.resolve("zeze.log"), line(5, "rotated-tail"), StandardOpenOption.APPEND);
			Files.move(directory.resolve("zeze.log"), directory.resolve("zeze.2026-09-28.log"));
			// Clock rollback puts the new generation's begin time before the unindexed rotated tail.
			AtomicFileWriter.replace(directory.resolve("zeze.log"),
					(line(2, "active-head") + line(6, "active-tail")).getBytes(StandardCharsets.UTF_8));
			var callback = Log4jFileManager.class.getDeclaredMethod("onFileCreated", java.nio.file.Path.class);
			callback.setAccessible(true);
			callback.invoke(manager, directory.resolve("zeze.2026-09-28.log"));
			assertEquals(2, manager.size());
			assertEquals(time(0), manager.entryAt(0).index.getEndTime(), "Only the old head was indexed");
			var session = new Log4jSession(manager);
			try {
				var search = new ArrayList<Log4jLog>();
				var browse = new LinkedList<Log4jLog>();
				boolean remain = switch (operation) {
					case 0 -> session.searchContains(search, time(4), time(7), List.of("tail"), BCondition.ContainsAll, 10);
					case 1 -> session.searchRegex(search, time(4), time(7), "tail", 10);
					case 2 -> session.browseContains(browse, time(4), time(7), List.of("tail"), BCondition.ContainsAll, 10, 0f);
					default -> session.browseRegex(browse, time(4), time(7), "tail", 10, 0f);
				};
				var result = operation < 2 ? search : browse;
				assertEquals(List.of(time(5), time(6)), result.stream().map(Log4jLog::getTime).toList(),
						"A sampled endTime must not hide an older generation's matching tail");
				assertFalse(remain);
			} finally {
				session.close();
			}
		} finally {
			if (manager != null)
				manager.stop();
			deleteBestEffort(directory);
		}
	}

	private static String line(long seconds, String text) {
		return BASE.plusSeconds(seconds).format(FORMAT) + " " + text + "\n";
	}

	private static long time(long seconds) {
		return BASE.plusSeconds(seconds).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
