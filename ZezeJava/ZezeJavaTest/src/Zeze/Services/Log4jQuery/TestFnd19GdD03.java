package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * GD-D03回归：服务端查询会话无空闲过期，CloseSession丢失即永久滞留（sid+walker+RAF慢泄漏）。
 * 修复后Log4jSession带lastActiveTime（查询进锁首行touchActive刷新），
 * ServerUserState.cleanIdleLogSessions由NewSession/查询路径顺带调用（惰性，无定期任务）：
 * 锁前查lastActiveTime不阻塞查询中的会话，锁内复核"查询中的会话不会过期"，
 * 超龄会话移除+close；<=0禁用；单个close失败只warn不中断。上限暂不做（拍板：过期覆盖主要风险）。
 */
@Fast
public class TestFnd19GdD03 {
	private static final long TimeoutMillis = 60_000;

	private Path logDir;
	private Log4jFileManager manager;

	@BeforeEach
	public void before() throws Exception {
		Task.tryInitThreadPool();
		logDir = Files.createTempDirectory("fnd19-gdd03-idle");
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();
		manager = new Log4jFileManager(logConf); // 空目录：会话仅包walker，不触文件
	}

	@AfterEach
	public void after() {
		manager.stop();
		deleteBestEffort(logDir);
	}

	/**
	 * 超龄会话被惰性清理（移除+close），活跃会话不动。
	 */
	@Test
	public void testIdleCleanedActiveKept() throws Exception {
		var state = new ServerUserState(null);
		var sessions = sessionsOf(state);
		var idle = new FixtureSession(TimeoutMillis + 1_000); // 超龄
		var active = new FixtureSession(0); // 活跃
		sessions.put(1L, idle);
		sessions.put(2L, active);

		state.cleanIdleLogSessions(TimeoutMillis);
		// 修复前：无任何清理出口，sid与Log4jSession永久滞留。
		assertTrue(idle.closed, "超龄会话应被close");
		assertFalse(sessions.containsKey(1L), "超龄会话应被移除");
		assertFalse(active.closed, "活跃会话不动");
		assertSame(active, sessions.get(2L));
	}

	/**
	 * touchActive刷新判定：被刷新过的"旧"会话不再过期（查询中的会话不会过期的判定基础）。
	 * 用真实Log4jSession反射置龄后touchActive，验证字段与清理判定的联动。
	 */
	@Test
	public void testTouchActiveRefreshSurvives() throws Exception {
		var state = new ServerUserState(null);
		var sessions = sessionsOf(state);
		var aged = new Log4jSession(manager);
		var refreshed = new Log4jSession(manager);
		sessions.put(1L, aged);
		sessions.put(2L, refreshed);
		ageLastActive(aged, TimeoutMillis + 1_000);
		ageLastActive(refreshed, TimeoutMillis + 1_000);

		// touchActive刷新后远新于阈值：语义与"等锁期间被并发查询刷新、锁内复核放行"一致。
		var before = refreshed.getLastActiveTime();
		refreshed.touchActive();
		assertTrue(refreshed.getLastActiveTime() >= before);

		state.cleanIdleLogSessions(TimeoutMillis);
		assertFalse(sessions.containsKey(1L), "未刷新的超龄会话仍应被清理");
		assertSame(refreshed, sessions.get(2L), "被touchActive刷新的会话不应过期");
	}

	/**
	 * 阈值<=0禁用清理（配置SessionIdleTimeoutMillis语义）。
	 */
	@Test
	public void testDisabledWhenNonPositive() throws Exception {
		var state = new ServerUserState(null);
		var sessions = sessionsOf(state);
		sessions.put(1L, new FixtureSession(Long.MAX_VALUE)); // 恒超龄

		state.cleanIdleLogSessions(0);
		assertEquals(1, sessions.size(), "阈值0应禁用清理");
		state.cleanIdleLogSessions(-1);
		assertEquals(1, sessions.size(), "负阈值应禁用清理");
	}

	/**
	 * 单个会话close失败：从map移除后close抛IOException只warn，不中断其余会话的处理，也不向外抛。
	 */
	@Test
	public void testCloseFailureTolerated() throws Exception {
		var state = new ServerUserState(null);
		var sessions = sessionsOf(state);
		var failToClose = new FixtureSession(TimeoutMillis + 1_000);
		failToClose.closeFail = new IOException("simulated close failure");
		var normal = new FixtureSession(TimeoutMillis + 1_000);
		sessions.put(1L, failToClose);
		sessions.put(2L, normal);

		assertDoesNotThrow(() -> state.cleanIdleLogSessions(TimeoutMillis));
		assertTrue(0 == sessions.size(), "close失败的会话也已从map移除");
		assertTrue(normal.closed, "其余超龄会话仍被正常清理");
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, Log4jSession> sessionsOf(ServerUserState state) throws Exception {
		Field field = ServerUserState.class.getDeclaredField("logSessions");
		field.setAccessible(true);
		return (ConcurrentHashMap<Long, Log4jSession>)field.get(state);
	}

	private static void ageLastActive(Log4jSession session, long ageMillis) throws Exception {
		Field field = Log4jSession.class.getDeclaredField("lastActiveTime");
		field.setAccessible(true);
		field.setLong(session, System.currentTimeMillis() - ageMillis);
	}

	/** 以距上次活跃的时长参与判定（GD-D03的清理只读getLastActiveTime，不触查询路径）。 */
	private final class FixtureSession extends Log4jSession {
		private final long ageMillis;
		boolean closed;
		IOException closeFail;

		FixtureSession(long ageMillis) {
			super(manager);
			this.ageMillis = ageMillis;
		}

		@Override
		public long getLastActiveTime() {
			return System.currentTimeMillis() - ageMillis;
		}

		@Override
		public void close() throws IOException {
			if (null != closeFail)
				throw closeFail;
			closed = true;
		}
	}
}
