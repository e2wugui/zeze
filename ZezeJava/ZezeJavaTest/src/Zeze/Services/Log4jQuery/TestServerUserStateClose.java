package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.Log4jQuery.ServerUserState;
import Zeze.Util.Task;

import harness.Fast;

/**
 * GD-C09回归：ServerUserState.close()首个会话close异常即中断循环，其余会话的
 * 文件句柄泄漏。修复后逐个try/catch收集异常（首异常+addSuppressed），循环结束
 * 统一抛并清空logSessions（对齐客户端SessionAll.close的既有写法）。
 */
@Fast
public class TestServerUserStateClose {
	private Path logDir;
	private Log4jFileManager manager;

	@BeforeEach
	public void before() throws Exception {
		Task.tryInitThreadPool();
		logDir = Files.createTempDirectory("fnd19-userstate-close");
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		manager = new Log4jFileManager(logConf);
	}

	@AfterEach
	public void after() {
		manager.stop();
		deleteBestEffort(logDir);
	}

	@Test
	public void testFirstCloseFailureDoesNotAbortRest() throws Exception {
		var state = new ServerUserState(null); // close路径不触logService
		var sessions = sessionsOf(state);
		var failing = new FixtureSession(new IOException("simulated close failure"));
		var normal1 = new FixtureSession(null);
		var normal2 = new FixtureSession(null);
		sessions.put(1L, failing);
		sessions.put(2L, normal1);
		sessions.put(3L, normal2);

		assertThrows(IOException.class, state::close);
		// 修复前：首个异常中断循环，normal1/normal2未close且map未清。
		assertTrue(normal1.closed, "首个会话close失败后，其余会话仍应被关闭");
		assertTrue(normal2.closed);
		assertEquals(0, sessions.size(), "close后应清空logSessions");

		assertDoesNotThrow(state::close); // 幂等：已清空
	}

	@Test
	public void testAllCleanCloseNoThrow() throws Exception {
		var state = new ServerUserState(null);
		var sessions = sessionsOf(state);
		var normal1 = new FixtureSession(null);
		var normal2 = new FixtureSession(null);
		sessions.put(1L, normal1);
		sessions.put(2L, normal2);

		assertDoesNotThrow(state::close);
		assertTrue(normal1.closed && normal2.closed);
		assertEquals(0, sessions.size());
	}

	/**
	 * FND25 log4j-02 回归：closeAsync（OnSocketClose 专用入口）不得同步等会话锁——
	 * 逐会话 close 与 Browse/Search 持同一会话锁互斥，慢扫描分钟级持锁下，selector 线程
	 * 同步等锁会把该 selector 上全部连接的 IO 钉停。会话表摘除必须同步完成（连接死亡即
	 * 逻辑不可查询），物理 close 投递专职线程。
	 */
	@Test
	public void testCloseAsyncDoesNotBlockOnHeldSessionLock() throws Exception {
		var state = new ServerUserState(null);
		var sessions = sessionsOf(state);
		var slow = new FixtureSession(null);
		sessions.put(1L, slow);

		// 另一线程持该会话的监视器锁 2s（等价"Browse 正在慢扫描"）。
		var entered = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var holder = Thread.ofPlatform().daemon().start(() -> {
			synchronized (slow) {
				entered.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS), "持锁线程必须就位");

		var begin = System.nanoTime();
		state.closeAsync();
		var elapsedMs = (System.nanoTime() - begin) / 1_000_000;
		assertTrue(elapsedMs < 800, "closeAsync不得同步等会话锁（selector钉停），elapsed=" + elapsedMs + "ms");
		assertEquals(0, sessions.size(), "会话表摘除必须在调用线程同步完成");

		release.countDown();
		holder.join(3000);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, Log4jSession> sessionsOf(ServerUserState state) throws Exception {
		Field field = ServerUserState.class.getDeclaredField("logSessions");
		field.setAccessible(true);
		return (ConcurrentHashMap<Long, Log4jSession>)field.get(state);
	}

	/** 只验证close路径的循环健壮性：close失败按需抛，成功记录closed标志。 */
	private final class FixtureSession extends Log4jSession {
		private final IOException failure;
		boolean closed;

		FixtureSession(IOException failure) {
			super(manager);
			this.failure = failure;
		}

		@Override
		public void close() throws IOException {
			if (failure != null)
				throw failure;
			closed = true;
		}
	}
}
