package Zeze.Transaction;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
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
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.TcpSocket;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real GCM, shared backend, terminal commit barrier and actual child-JVM halt. */
@Fast
@Timeout(60)
public class TestGlobalFatalSaveWindow {

	@TempDir Path temp;
	static final AtomicInteger nextId = new AtomicInteger(FastServerIds.TEST_GLOBAL_FATAL_HALT + 1);
	static final class Node implements AutoCloseable {
		final Application app;
		final TestFormalCheckpoint.Values table = new TestFormalCheckpoint.Values();
		Node(int id, int port, String url) throws Exception {
			var c = new Config();
			c.setServerId(id);
			c.setServiceManager("disable");
			c.setDefaultTableConf(new Config.TableConf());
			c.setCheckpointPeriod(3_600_000);
			c.setCheckpointFlushMode(CheckpointFlushMode.SingleThread);
			c.setGlobalCacheManagerHostNameOrAddress("127.0.0.1");
			c.setGlobalCacheManagerPort(port);
			var db = new Config.DatabaseConf();
			db.setDatabaseUrl(url);
			c.getDatabaseConfMap().put("", db);
			app = new Application("FatalSaveWindow" + id, c);
			app.addTable("", table);
			app.start();
		}
		void put(long n) { TestFormalCheckpoint.put(app, table, 1, n); }
		GlobalAgentBase agent() { return Objects.requireNonNull(app.getGlobalAgent()).getAgent(0); }
		@Override public void close() throws Exception { app.stop(); }
	}

	static int port(GlobalCacheManagerAsyncServer gcm) throws Exception {
		var f = GlobalCacheManagerAsyncServer.class.getDeclaredField("serverSocket");
		f.setAccessible(true);
		return Objects.requireNonNull(((TcpSocket)f.get(gcm)).getLocalInet()).getPort();
	}

	// Only wrap commit to record externally observable evidence AFTER the shared DB accepted the write.
	static void recordCommits(TestFormalCheckpoint.Values table) throws Exception {
		var target = Objects.requireNonNull(table.getStorage()).getDatabaseTable();
		var delegate = target.getDatabase();
		class WitnessDb extends Database {
			WitnessDb() { super(null, new Config.DatabaseConf()); }
			final class Tx implements Database.Transaction {
				final Database.Transaction inner = delegate.beginTransaction();
				@Override public void commit() {
					inner.commit();
					try { Files.writeString(Path.of("saved"), Long.toString(TestFormalCheckpoint.disk(table, 1))); }
					catch (Exception e) { throw new AssertionError(e); }
				}
				@Override public void rollback() { inner.rollback(); }
				@Override public void close() throws Exception { inner.close(); }
			}
			@Override public Database.Transaction beginTransaction() { return new Tx(); }
			@Override public Database.Table openTable(String name, int id) {
				return (Database.Table)Proxy.newProxyInstance(Database.Table.class.getClassLoader(),
						new Class<?>[]{Database.Table.class}, (proxy, method, args) -> {
							if (method.getName().equals("getDatabase")) return this;
							if (args != null && args.length > 0 && args[0] instanceof Tx tx) args[0] = tx.inner;
							try { return method.invoke(target, args); }
							catch (InvocationTargetException e) { throw e.getCause(); }
						});
			}
		}
		var db = new WitnessDb();
		table.setDatabase(db);
		var f = TableX.class.getDeclaredField("storage"); f.setAccessible(true);
		f.set(table, new Storage<>(table, db, table.getName()));
	}

	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();
		var gcm = new GlobalCacheManagerAsyncServer();
		gcm.start(InetAddress.getLoopbackAddress(), 0, new Config());
		var n = new Node(FastServerIds.TEST_GLOBAL_FATAL_HALT, port(gcm), "fatal_child_shared");
		n.put(17);
		recordCommits(n.table);
		Objects.requireNonNull(n.app.getAchillesHeelDaemon()).stopAndJoin();
		var r = Objects.requireNonNull(Objects.requireNonNull(n.table.getCache()).get(1L));
		var v = (TestFormalCheckpoint.Value)Objects.requireNonNull(r.getDirtyValue());
		if (args[0].equals("blocked"))
			v.afterEncode = () -> TestFormalCheckpoint.await(new CountDownLatch(1));
		else {
			var attempts = new AtomicInteger();
			v.afterEncode = () -> {
				// An already-running transaction and a new attempt cannot publish behind the final snapshot.
				assertEquals(Procedure.Closed, n.app.newProcedure(() -> {
					n.table.put(1L, new TestFormalCheckpoint.Value(99)); return 0;
				}, "fatal.rejected").call());
				try { Thread.sleep(1000); } catch (InterruptedException e) { throw new AssertionError(e); }
				assertEquals(1, attempts.incrementAndGet());
			};
		}
		var agent = n.agent();
		var cfg = agent.getConfig();
		agent.setActiveTime(System.currentTimeMillis() - cfg.serverDaemonTimeout - cfg.serverReleaseTimeout + 5000);
		if (args[0].equals("release")) {
			// Keep the real Releaser waiting on Lockey while the final checkpoint saves the committed value.
			var key = n.app.getLocks().get(new TableKey(n.table.getId(), 1L));
			key.enterWriteLock();
			agent.startRelease(n.app, null);
			assertEquals(GlobalAgentBase.CheckReleaseResult.Timeout,
					agent.checkReleaseTimeout(System.currentTimeMillis(), cfg.serverReleaseTimeout));
			long deadline = agent.getFatalSaveDeadline();
			agent.setActiveTime(System.currentTimeMillis());
			assertEquals(deadline, agent.getFatalSaveDeadline(), "late heartbeat must not renew stuck release");
		}
		Files.writeString(Path.of("ready"), Long.toString(agent.getFatalSaveDeadline()));
		Application.haltAfterCheckpoint(n.app, 73);
	}

	String classpath() throws Exception {
		var entries = new LinkedHashSet<String>();
		for (var loader = getClass().getClassLoader(); loader != null; loader = loader.getParent())
			if (loader instanceof URLClassLoader urls)
				for (var url : urls.getURLs()) if (url.getProtocol().equals("file"))
					entries.add(Path.of(url.toURI()).toAbsolutePath().toString());
		for (var s : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
			entries.add(Path.of(s).toAbsolutePath().toString());
		return String.join(File.pathSeparator, entries);
	}

	void child(String mode, boolean saved) throws Exception {
		var dir = Files.createDirectory(temp.resolve(mode));
		var args = dir.resolve("java.args");
		Files.write(args, List.of("-ea", "-Dzeze.test.env=off", "-cp", classpath(), getClass().getName(), mode)
				.stream().map(s -> '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"').toList(),
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		var log = dir.resolve("child.log");
		var p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(), "@" + args)
				.directory(dir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			assertTrue(p.waitFor(25, TimeUnit.SECONDS), "child must terminate within its protected window");
			assertEquals(73, p.exitValue(), () -> {
				try { return Files.readString(log); } catch (Exception e) { return e.toString(); }
			});
			assertTrue(Files.exists(dir.resolve("ready")));
			long deadline = Long.parseLong(Files.readString(dir.resolve("ready")));
			long observedExit = System.currentTimeMillis();
			// 容差2s在满载soak下被父fork调度延迟击穿（子JVM行为全对，soak-20261118-1009 R5实锤），
			// 测的是父侧观察延迟非子行为，10s裕度。
			assertTrue(observedExit <= deadline + 10_000, "child exceeded protected deadline plus observation tolerance");
			assertEquals(saved, Files.exists(dir.resolve("saved")));
			if (saved) assertEquals("17", Files.readString(dir.resolve("saved")));
			// Retain full child evidence independently of JUnit's temporary directory cleanup.
			var archive = Path.of("../formal/integration/build/fatal-redo/children", mode);
			Files.createDirectories(archive);
			Files.copy(log, archive.resolve("child.log"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			Files.writeString(archive.resolve("timing.txt"), "deadline=" + deadline + "\nobservedExit=" + observedExit);
			if (saved) Files.copy(dir.resolve("saved"), archive.resolve("saved"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} finally { if (p.isAlive()) { p.destroyForcibly(); p.waitFor(5, TimeUnit.SECONDS); } }
	}

	@Test void nonReleaseFatalActuallySavesBeforeHalt() throws Exception { child("fatal", true); }
	@Test void releaseTimeoutStillHasAProtectedSavingWindow() throws Exception { child("release", true); }
	@Test void permanentlyBlockedSavingStillExitsBeforeReclaim() throws Exception { child("blocked", false); }

	@Test void temporaryBackendFailureRetriesThenHandoffCannotReplayOldValue() throws Exception {
		Task.tryInitThreadPool();
		var gcm = new GlobalCacheManagerAsyncServer();
		gcm.start(InetAddress.getLoopbackAddress(), 0, new Config());
		int id = nextId.getAndAdd(2);
		try (var a = new Node(id, port(gcm), "fatal_handoff_" + id);
			 var b = new Node(id + 1, port(gcm), "fatal_handoff_" + id);
			 var pool = Executors.newFixedThreadPool(2)) {
			a.put(41);
			var db = TestFormalCheckpoint.inject(a.table);
			db.failuresLeft = 5; // Exhaust the first flush's four attempts; final-save loop must retry.
			var read = new CountDownLatch(1);
			var resume = new CountDownLatch(1);
			var late = pool.submit(() -> a.app.newProcedure(() -> {
				assertNotNull(a.table.get(1L)); read.countDown();
				assertTrue(resume.await(10, TimeUnit.SECONDS));
				a.table.put(1L, new TestFormalCheckpoint.Value(99)); return 0;
			}, "fatal.alreadyRunning").call());
			assertTrue(read.await(10, TimeUnit.SECONDS));
			var cp = Objects.requireNonNull(a.app.getCheckpoint());
			assertTrue(cp.tryBeginCommit()); // An admitted publisher still owns a commit permit.
			var saver = pool.submit(() -> { cp.saveBeforeFatalExit(); return null; });
			try {
				var field = Checkpoint.class.getDeclaredField("commitState"); field.setAccessible(true);
				var state = (AtomicInteger)field.get(cp);
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
				while (state.get() < 0 && System.nanoTime() < until) Thread.sleep(5);
				assertTrue(state.get() >= 0, "saving must close admission");
				assertFalse(saver.isDone(), "final snapshot must wait for admitted publishers");
				assertEquals(-1, TestFormalCheckpoint.disk(a.table, 1));
			} finally { cp.endCommit(); resume.countDown(); }
			saver.get(15, TimeUnit.SECONDS);
			assertEquals(Procedure.Closed, late.get(10, TimeUnit.SECONDS));
			assertTrue(db.attempts >= 6);
			assertEquals(41, TestFormalCheckpoint.disk(a.table, 1));
			b.put(52); // real Reduce and acquire from the now quiescent A
			b.app.checkpointRun();
			a.app.checkpointRun();
			assertEquals(52, TestFormalCheckpoint.disk(b.table, 1));
			assertEquals(Procedure.Closed, a.app.newProcedure(() -> {
				a.table.put(1L, new TestFormalCheckpoint.Value(53)); return 0;
			}, "fatal.newWriter").call());
			a.app.checkpointRun();
			assertEquals(52, TestFormalCheckpoint.disk(b.table, 1));
		} finally { gcm.stop(); }
	}
}
