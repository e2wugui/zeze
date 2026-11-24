package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Logs.LogLong;
import harness.Fast;
import harness.FastServerIds;
import harness.proc.Procs;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestListenerCollectionFailureAfterApply {
	@Test
	void assertionAfterApplyUsesFatalSave(@TempDir Path directory) throws Exception {
		var classpath = new LinkedHashSet<String>();
		for (var loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs())
					classpath.add(Path.of(url.toURI()).toString());
			}
		}
		classpath.addAll(List.of(System.getProperty("java.class.path").split(File.pathSeparator)));
		var output = directory.resolve("listener-collection.log");
		var child = new ProcessBuilder(Procs.JAVAW, "-ea", "-cp", String.join(File.pathSeparator, classpath),
				Probe.class.getName(), Integer.toString(FastServerIds.takeoverPoolNext()))
				.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue(child.waitFor(45, TimeUnit.SECONDS), "fatal save did not terminate the process");
			var log = Files.readString(output);
			assertTrue(log.contains("APPLIED_BEFORE_COLLECTION"), log);
			assertFalse(log.contains("COMMIT_RETURNED"), log);
			var expected = System.getProperty("os.name").startsWith("Windows") ? 543543 : 543543 & 255;
			assertEquals(expected, child.exitValue(), log);
		} finally {
			if (child.isAlive())
				child.destroyForcibly().waitFor();
		}
	}

	public static class Probe {
		public static void main(String[] args) throws Exception {
			var conf = new Config();
			conf.setServerId(Integer.parseInt(args[0]));
			conf.setCheckpointMode(CheckpointMode.Table);
			conf.setCheckpointPeriod(3_600_000);
			conf.setServiceManager("disable");
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var db = new Config.DatabaseConf();
			db.setDatabaseUrl("listener_collection_failure_" + conf.getServerId());
			conf.getDatabaseConfMap().put("", db);
			var app = new Application("ListenerCollectionFailure", conf);
			app.start();
			var table = new TableDynamic<>(app, "listener_collection_records", (Long key) -> {
				var bb = ByteBuffer.Allocate(9);
				bb.WriteLong(key);
				return bb;
			}, ByteBuffer::ReadLong, FailingBean::new, false, null, Long.class, FailingBean.class);
			table.getChangeListenerMap().addListener((key, change) -> { });
			var handle = MethodHandles.lookup().findVarHandle(FailingBean.class, "value", long.class);
			app.newProcedure(() -> {
				var row = table.getOrAdd(1L);
				Transaction.getCurrentVerifyWrite(row).putLog(new LogLong(row, 1, handle, 10));
				return Procedure.Success;
			}, "ListenerCollectionFailure.write").call();
			System.out.println("COMMIT_RETURNED");
			Runtime.getRuntime().halt(99);
		}
	}

	private static final class FailingBean extends EmptyBean {
		private long value;

		@Override
		public LogBean createLogBean(@Nullable TransactionLog finalLog) {
			if (value == 10) {
				System.out.println("APPLIED_BEFORE_COLLECTION");
				System.out.flush();
			}
			throw new AssertionError("listener log collection failed");
		}
	}
}
