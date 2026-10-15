package Zeze.Transaction;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import harness.proc.Procs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** 真子进程验证 fatal 分支，不能用测试线程的异常断言代替进程终止语义。 */
@Fast
public class TestHistoryCaptureFatal {
	@TempDir
	Path dir;

	@Test
	public void captureFailureAlwaysHaltsBeforeReportingSuccess() throws Exception {
		var classpath = new LinkedHashSet<String>();
		for (var loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs())
					classpath.add(Path.of(url.toURI()).toString());
			}
		}
		classpath.addAll(java.util.List.of(System.getProperty("java.class.path").split(File.pathSeparator)));
		for (var mode : CheckpointMode.values()) {
			for (var error : new String[]{"exception", "assertion"}) {
				var output = dir.resolve(mode + "_" + error + ".log");
				var child = new ProcessBuilder(Procs.JAVAW, "-ea", "-cp", String.join(File.pathSeparator, classpath),
						Probe.class.getName(), mode.name(), error, Integer.toString(FastServerIds.takeoverPoolNext()))
						.redirectErrorStream(true).redirectOutput(output.toFile()).start();
				try {
					assertTrue(child.waitFor(45, TimeUnit.SECONDS), "History fatal 子进程不得挂起");
					var log = Files.readString(output);
					assertTrue(log.contains("CAPTURE_ENTERED"), log);
					assertFalse(log.contains("COMMIT_RETURNED"), log);
					var expected = System.getProperty("os.name").startsWith("Windows") ? 543543 : 543543 & 255;
					assertEquals(expected, child.exitValue(), log);
				} finally {
					if (child.isAlive())
						child.destroyForcibly().waitFor();
				}
			}
		}
	}

	public static class Probe {
		public static void main(String[] args) throws Exception {
			var conf = new Config();
			conf.setServerId(Integer.parseInt(args[2]));
			conf.setCheckpointMode(CheckpointMode.valueOf(args[0]));
			conf.setCheckpointPeriod(3_600_000);
			conf.setServiceManager("disable");
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var db = new Config.DatabaseConf();
			db.setDatabaseUrl("capture_fatal_" + conf.getServerId());
			conf.getDatabaseConfMap().put("", db);
			var app = new Application("CaptureFatal", conf);
			var table = new demo.Module1.Table3();
			app.addTable("", table);
			var history = new IHistory() {
				@Override
				public void registerLogs(Iterable<Table> tables) {
				}

				@Override
				public void afterOpen() {
				}

				@Override
				public PreparedCommit prepareCommit(Procedure procedure) {
					return changes -> {
						System.out.println("CAPTURE_ENTERED");
						System.out.flush();
						if (args[1].equals("assertion"))
							throw new AssertionError("injected capture assertion");
						throw new IllegalStateException("injected capture exception");
					};
				}

				@Override
				public void close() {
				}
			};
			var field = Application.class.getDeclaredField("history");
			field.setAccessible(true);
			field.set(app, history);
			app.start();
			app.newProcedure(() -> {
				table.getOrAdd(1L).setLong2(10);
				return 0;
			}, "captureMustHalt").call();
			System.out.println("COMMIT_RETURNED");
			Runtime.getRuntime().halt(99);
		}
	}
}
