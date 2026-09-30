package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.LogAgent;
import Zeze.Util.Task;

import harness.Fast;

/**
 * LogAgentManager.stop 的会话绑定表清理直测：绑定表（map）是 FileSessionManager 的
 * 类级静态，stop 只回收 httpServer/adminNetty/logAgent 时不被触碰——嵌入宿主
 * stop→init 后同 IP 同参数请求经 matches（纯四元组 + 新 agent 注册表）复用持有
 * 已停 agent 的死会话（operate 时死 Connector 上 GetReadySocket 5s 超时，非会话级
 * 异常不触发重建，恒 system error），且复用路径在 operate 之前刷新 lastActiveNanos
 * 使 2h 闲置清扫对持续重试的客户端永不命中。修复后 stop 清空绑定表并经
 * closeExecutor 异步关闭会话，重启后同 IP 请求按需干净重建。
 *
 * <p>@Isolated：写 FileSessionManager/LogAgentManager JVM 级静态状态，独占运行。</p>
 *
 * <p>源 IP 段（127.0.0.N 末字节）：63/64。段是全局共享资源——TestSessionLevelErrorTriage
 * （Services/Log4jQuery 包）占 61/62 且按"惰性清扫域自管理"设计故意留 map 残留，本类首占
 * 61 时跨类先后跑必撞"首次入库无替换"假红（2026-09-30 IDEA 全量轮实证；对齐 serverId
 * 全局唯一铁律：占段前全树 grep clientAddr 盘点，已占 51/52/55/56/58/59/60/61/62）。</p>
 */
@Fast
@Isolated
public class TestStopClearsFileSessionBindings {

	/** 会话身份条件指纹样本（值任意，比对按值等价）。 */
	private static final String COND = "search|-1|-1|1|[error]|";

	/** stop 必须清空绑定表并异步关闭全部会话（停机不滞留服务端查询句柄的客户端侧收口）。 */
	@Test
	public void testStopEvictsAllBindingsAndClosesSessions() throws Exception {
		Task.tryInitThreadPool();
		var session = new CloseCountingSession();
		var addr = clientAddr(63);
		assertNull(FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", COND, session)),
				"摆盘：绑定首次入库无替换");
		try {
			LogAgentManager.stop();
			assertNull(FileSessionManager.get(addr), "stop 必须清空静态绑定表（重启后不得复用死会话）");
			assertTrue(session.closed.await(5, TimeUnit.SECONDS),
					"被清绑定的会话必须经 closeExecutor 异步关闭（服务端句柄释放）");
		} finally {
			clearBindingsTable();
		}
	}

	/** stop→重启形态：同 IP 同参数请求不得复用旧 agent 的会话，必须对新 agent 干净重建。 */
	@Test
	public void testRequestAfterStopRebuildsInsteadOfReusingStaleSession() throws Exception {
		Task.tryInitThreadPool();
		var stale = newUninitialized(StubSession.class);
		var addr = clientAddr(64);
		FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", COND, stale));
		var agent = newUninitialized(StubLogAgent.class);
		try {
			LogAgentManager.stop();
			var resolved = FileSessionManager.resolve(agent, addr, false, false, "game1", "zeze", COND);
			assertNotSame(stale, resolved, "stop 后旧绑定不得复用（死会话恒 system error 的毒化源）");
			assertEquals(1, agent.newSessionCalls, "重启后同 IP 请求必须走干净重建");
		} finally {
			clearBindingsTable();
		}
	}

	// ---------------------------------------------------------------- helpers

	private static SocketAddress clientAddr(int lastOctet) throws Exception {
		return new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, (byte)lastOctet}), 12345);
	}

	/** 修复前红跑的绑定残留兜底清理（修复后 stop 即清，此兜底为空操作）。 */
	@SuppressWarnings("unchecked")
	private static void clearBindingsTable() throws Exception {
		Field mapField = FileSessionManager.class.getDeclaredField("map");
		mapField.setAccessible(true);
		((java.util.Map<String, LogSessionBinding>)mapField.get(null)).clear();
	}

	/** 最小可关闭会话桩：记录 close 被调用（closeExecutor 异步，闩同步）。 */
	private static final class CloseCountingSession implements AutoCloseable {
		final CountDownLatch closed = new CountDownLatch(1);

		@Override
		public void close() {
			closed.countDown();
		}
	}

	private static final class StubSession extends Session {
		@SuppressWarnings("unused")
		StubSession() {
			super(null, null, null);
		}

		@Override
		public void close() {
			// 不触真实 RPC。
		}
	}

	/** registry 可变（模拟重启后的新 agent 注册表）；newSession 计数（重建判定）。 */
	private static final class StubLogAgent extends LogAgent {
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		int newSessionCalls;

		@Override
		public Session newSession(String serverName, String logName) {
			++newSessionCalls;
			try {
				return newUninitialized(StubSession.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Set<String> getLogServers() {
			return Set.of("game1");
		}
	}

	/** 不调构造器分配实例（字段全默认值）：Session/LogAgent 的构造器都触真实 RPC。 */
	@SuppressWarnings("restriction")
	private static <T> T newUninitialized(Class<T> clazz) throws Exception {
		@SuppressWarnings("unchecked")
		Constructor<T> ctor = (Constructor<T>)sun.reflect.ReflectionFactory.getReflectionFactory()
				.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
		return ctor.newInstance();
	}
}
