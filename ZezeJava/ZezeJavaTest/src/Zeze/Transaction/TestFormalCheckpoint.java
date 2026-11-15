package Zeze.Transaction;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** S4/S5 implementation witnesses; backend is real RocksDB, no SM/GCM. */
@Fast
@Timeout(180)
public class TestFormalCheckpoint {

	@TempDir Path temp;

	static final class Value extends Bean {
		long value;
		@Nullable Runnable afterEncode;
		boolean poison;
		Value(long value) { this.value = value; }
		@Override public void encode(ByteBuffer bb) {
			bb.WriteLong(value); // freeze old value before the controlled interleaving
			if (poison) throw new IllegalStateException("formal encoding failure");
			var hook = afterEncode;
			afterEncode = null;
			if (hook != null) hook.run();
		}
		@Override public void decode(IByteBuffer bb) { value = bb.ReadLong(); }
		@Override public Value copy() { return new Value(value); }
	}
	static final class Values extends TableX<Long, Value> {
		Values() { super(991970, "TestFormalCheckpoint_Values"); }
		@Override public boolean isMemory() { return false; }
		@Override public Class<Long> getKeyClass() { return Long.class; }
		@Override public Class<Value> getValueClass() { return Value.class; }
		@Override public Value newValue() { return new Value(0); }
		@Override public Long decodeKey(ByteBuffer bb) { return bb.ReadLong(); }
		@Override public ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate(); bb.WriteLong(key); return bb;
		}
		@Override public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException { return rs.getLong("__key"); }
		@Override public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long key) { st.appendLong("__key", key); }
	}
	static Application start(Path path, Values table, String mode, int offset) throws Exception {
		var c = new Config();
		c.setServerId(FastServerIds.TEST_FORMAL_CHECKPOINT + offset);
		c.setServiceManager("disable");
		c.setCheckpointMode(mode.equals("immediate") ? CheckpointMode.Immediately : CheckpointMode.Table);
		c.setCheckpointPeriod(3_600_000);
		var tc = new Config.TableConf();
		tc.setCheckpointWhenCommit(mode.equals("whenCommit"));
		c.setDefaultTableConf(tc);
		var db = new Config.DatabaseConf();
		db.setDatabaseType(Config.DbType.RocksDb);
		db.setDatabaseUrl(path.toAbsolutePath().toString());
		// Hard halt leaves InUse; this standalone deployment has no coordination.
		db.setDisableOperates(true);
		c.getDatabaseConfMap().put("", db);
		var app = new Application("TestFormalCheckpoint" + offset, c);
		app.addTable("", table);
		app.start();
		return app;
	}
	static Record1<Long, Value> put(Application app, Values table, long key, long value) {
		assertEquals(0, app.newProcedure(() -> { table.put(key, new Value(value)); return 0; }, "formal.put").call());
		return Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(key));
	}
	static void flush(Application app, Record r) {
		RelativeRecordSet.flush(Objects.requireNonNull(app.getCheckpoint()), r.getRelativeRecordSet());
	}
	static long disk(Values t, long key) {
		var v = t.selectFromDatabase(key); return v != null ? v.value : -1;
	}
	static void await(CountDownLatch latch) {
		try { assertTrue(latch.await(10, TimeUnit.SECONDS)); }
		catch (InterruptedException e) { throw new AssertionError(e); }
	}

	@Test void oldFlushFinishesBeforeTheNextSameGroupCommit() throws Exception {
		var table = new Values();
		var app = start(temp.resolve("ordered"), table, "async", 0);
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var pool = Executors.newFixedThreadPool(2);
		try {
			var r = put(app, table, 1, 1);
			((Value)Objects.requireNonNull(r.getDirtyValue())).afterEncode = () -> { entered.countDown(); await(release); };
			var f = pool.submit(() -> flush(app, r));
			await(entered);
			var writerThread = new AtomicReference<Thread>();
			var w = pool.submit(() -> {
				writerThread.set(Thread.currentThread());
				return put(app, table, 1, 2);
			});
			long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			boolean blockedAtRrs = false;
			while (System.nanoTime() < until && !w.isDone()) {
				var thread = writerThread.get();
				if (thread != null)
					for (var frame : thread.getStackTrace())
						if (frame.getClassName().equals(RelativeRecordSet.class.getName())
								&& frame.getMethodName().equals("lockCommit")) blockedAtRrs = true;
				if (blockedAtRrs) break;
				Thread.yield();
			}
			assertTrue(blockedAtRrs, "writer must wait on the held RRS lock");
			assertFalse(w.isDone());
			release.countDown();
			f.get(10, TimeUnit.SECONDS); w.get(10, TimeUnit.SECONDS);
			assertEquals(1, disk(table, 1));
			assertTrue(r.getDirty(), "the later commit remains dirty");
			flush(app, r);
			assertEquals(2, disk(table, 1));
			assertFalse(r.getDirty());
		} finally { release.countDown(); pool.close(); app.stop(); }
	}

	@Test void failedEncodingRetainsTheWholeGroupAndRetriesLatestValues() throws Exception {
		var table = new Values(); var app = start(temp.resolve("retry"), table, "async", 1);
		try {
			assertEquals(0, app.newProcedure(() -> {
				table.put(1L, new Value(11)); table.put(2L, new Value(12)); return 0;
			}, "formal.group").call());
			var r = Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(1L));
			var r2 = Objects.requireNonNull(table.getCache().get(2L));
			assertSame(r.getRelativeRecordSet(), r2.getRelativeRecordSet());
			var value = (Value)Objects.requireNonNull(r2.getDirtyValue()); value.poison = true;
			app.checkpointRun(); // Deliberately catches failed units and returns normally.
			assertEquals(-1, disk(table, 1)); assertEquals(-1, disk(table, 2));
			assertTrue(r.getDirty()); assertTrue(r2.getDirty());
			assertTrue(Objects.requireNonNull(app.getCheckpoint()).relativeRecordSetMap.contains(r.getRelativeRecordSet()));
			value.poison = false;
			put(app, table, 1, 21);
			app.checkpointRun();
			assertEquals(21, disk(table, 1)); assertEquals(12, disk(table, 2));
			assertFalse(r.getDirty()); assertFalse(r2.getDirty());
		} finally { app.stop(); }
	}

	@Test void rollbackNeverEntersTheCheckpointSnapshot() throws Exception {
		var table = new Values(); var app = start(temp.resolve("rollback"), table, "async", 2);
		try {
			var r = put(app, table, 1, 31);
			assertEquals(7, app.newProcedure(() -> { table.put(1L, new Value(99)); return 7; }, "formal.rollback").call());
			flush(app, r); assertEquals(31, disk(table, 1));
		} finally { app.stop(); }
	}
	@Test void independentWritesCanPersistOutsideGlobalCommitOrderButDependenciesMerge() throws Exception {
		var table = new Values(); var app = start(temp.resolve("groups"), table, "async", 5);
		try {
			put(app, table, 1, 1); var second = put(app, table, 2, 2);
			flush(app, second);
			assertEquals(-1, disk(table, 1)); assertEquals(2, disk(table, 2));
			var third = put(app, table, 3, 3);
			assertEquals(0, app.newProcedure(() -> {
				assertNotNull(table.get(3L)); table.put(4L, new Value(4)); return 0;
			}, "formal.dependency").call());
			var fourth = Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(4L));
			assertSame(third.getRelativeRecordSet(), fourth.getRelativeRecordSet());
			flush(app, fourth);
			assertEquals(3, disk(table, 3)); assertEquals(4, disk(table, 4));
		} finally { app.stop(); }
	}

	/** Wrap only the primary storage boundary, preserving the actual RocksDB transaction. */
	static final class FaultDatabase extends Database {
		final Database delegate;
		final Database.Table target;
		int failuresLeft;
		int attempts;
		String crashAt = "";
		FaultDatabase(Values table) {
			super(null, new Config.DatabaseConf());
			target = Objects.requireNonNull(table.getStorage()).getDatabaseTable();
			delegate = target.getDatabase();
		}
		final class Tx implements Database.Transaction {
			final Database.Transaction inner = delegate.beginTransaction();
			@Override public void commit() {
				attempts++;
				if (crashAt.equals("beforeDb")) halt(crashAt);
				if (failuresLeft-- > 0) throw new IllegalStateException("formal transient conflict");
				inner.commit();
				if (crashAt.equals("afterDb")) halt(crashAt);
			}
			@Override public void rollback() { inner.rollback(); }
			@Override public void close() throws Exception { inner.close(); }
		}
		@Override public Database.Transaction beginTransaction() { return new Tx(); }
		@Override public boolean isTransientWriteConflict(Throwable e) {
			return e instanceof IllegalStateException && e.getMessage().equals("formal transient conflict");
		}
		@Override public Database.Table openTable(String name, int id) {
			return (Database.Table)Proxy.newProxyInstance(Database.Table.class.getClassLoader(),
					new Class<?>[]{Database.Table.class}, (proxy, method, args) -> {
						if (method.getName().equals("getDatabase")) return this;
						if (args != null && args.length > 0 && args[0] instanceof FaultDatabase.Tx tx)
							args[0] = tx.inner;
						try { return method.invoke(target, args); }
						catch (InvocationTargetException e) { throw e.getCause(); }
					});
		}
	}
	static FaultDatabase inject(Values table) throws Exception {
		var db = new FaultDatabase(table); table.setDatabase(db);
		var field = TableX.class.getDeclaredField("storage"); field.setAccessible(true);
		field.set(table, new Storage<>(table, db, table.getName())); return db;
	}
	@Test void transientDatabaseFailureReplaysTheSnapshotInANewTransaction() throws Exception {
		var table = new Values(); var app = start(temp.resolve("db-retry"), table, "async", 3);
		try {
			var r = put(app, table, 1, 41); var db = inject(table); db.failuresLeft = 1;
			flush(app, r); assertEquals(2, db.attempts);
			assertEquals(41, disk(table, 1)); assertFalse(r.getDirty());
		} finally { app.stop(); }
	}

	static void halt(String point) {
		System.out.println("CRASH " + point); System.out.flush(); Runtime.getRuntime().halt(73);
	}
	public static void main(String[] args) throws Exception {
		String mode = args[0], point = args[1];
		var table = new Values(); var app = start(Path.of("primary"), table, mode, 4);
		if (point.equals("verify")) {
			assertEquals(Long.parseLong(args[2]), disk(table, 1));
			assertEquals(Long.parseLong(args[2]), disk(table, 2));
			assertEquals(0, app.newProcedure(() -> {
				var a = table.get(1L); var b = table.get(2L);
				assertEquals(Long.parseLong(args[2]), a != null ? a.value : -1);
				assertEquals(Long.parseLong(args[2]), b != null ? b.value : -1); return 0;
			}, "formal.reload").call());
			app.stop(); System.out.println("VERIFIED"); return;
		}
		if (point.equals("beforeDb") || point.equals("afterDb")) inject(table).crashAt = point;
		assertEquals(0, app.newProcedure(() -> {
			var a = new Value(51); var b = new Value(51);
			if (point.equals("encoded")) a.afterEncode = () -> halt(point);
			table.put(1L, a); table.put(2L, b); return 0;
		}, "formal.child.commit").call());
		if (point.equals("response")) halt(point);
		var r = Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(1L));
		flush(app, r);
		assertFalse(r.getDirty()); halt(point); // after clean, without stop/shutdown hooks
	}
	private String classpath() throws Exception {
		var entries = new LinkedHashSet<String>();
		for (var loader = getClass().getClassLoader(); loader != null; loader = loader.getParent())
			if (loader instanceof URLClassLoader urls)
				for (var url : urls.getURLs())
					if (url.getProtocol().equals("file")) entries.add(Path.of(url.toURI()).toAbsolutePath().toString());
		for (var entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
			entries.add(Path.of(entry).toAbsolutePath().toString());
		return String.join(File.pathSeparator, entries);
	}
	private void child(Path dir, String mode, String point, long expected, int exit) throws Exception {
		var arguments = List.of("-ea", "-Dzeze.test.env=off", "-cp", classpath(), getClass().getName(), mode, point, Long.toString(expected));
		var argFile = dir.resolve("java.args");
		Files.write(argFile, arguments.stream().map(s -> '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"').toList(),
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		var log = dir.resolve(point + ".log");
		var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(), "@" + argFile)
				.directory(dir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			assertTrue(process.waitFor(30, TimeUnit.SECONDS), "child timeout: " + log);
			assertEquals(exit, process.exitValue(), () -> { try { return Files.readString(log); } catch (Exception e) { return e.toString(); } });
			assertTrue(Files.readString(log).contains(exit == 73 ? "CRASH " + point : "VERIFIED"));
		} finally { if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); } }
	}
	@Test void hardCrashAndRestartRetainOnlyTheCommittedBackend() throws Exception {
		for (var mode : List.of("async", "immediate", "whenCommit"))
			for (var point : List.of("encoded", "beforeDb", "afterDb", "clean", "response")) {
				var dir = Files.createDirectory(temp.resolve(mode + "-" + point));
				long expected = point.equals("encoded") || point.equals("beforeDb") || mode.equals("async") && point.equals("response") ? -1 : 51;
				child(dir, mode, point, expected, 73); child(dir, mode, "verify", expected, 0);
				System.out.println("CRASH_RESTART_VERIFIED mode=" + mode + " point=" + point + " value=" + expected);
			}
	}
}
