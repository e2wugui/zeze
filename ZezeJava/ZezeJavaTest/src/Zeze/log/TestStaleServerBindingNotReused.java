package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.LogAgent;
import Zeze.Util.Task;

import harness.Fast;

/**
 * FileSessionManager.resolve 的续页期摘册路径直测：绑定三元组命中的服务器被 SM
 * 摘除（Client.onSmRemoved 从注册表移除 Connector）后，复用判据若只比静态三元组，
 * 死绑定恒复用——Session 内对已摘册名的失败每页必现直到 2h 闲置清扫，滞留绑定的
 * 服务端会话句柄不被释放。修复后单服务器视图复用前置校验"目标仍在注册表"：
 * 摘册即视同 changeSession 走重建（重建对未注册名显式失败，错误可见且可诊断）；
 * 全服视图的摘册收敛由 allViewMembersConverged 既有承担（TestAllViewMemberConvergence）。
 * stub 形制对齐 TestSessionAllOperatePartialFailure：ReflectionFactory 不调构造器
 * 分配实例（Session/LogAgent 构造均触真实 RPC）。
 *
 * <p>@Isolated：写 FileSessionManager 静态表（独立源 IP 隔离），独占运行。</p>
 */
@Fast
@Isolated
public class TestStaleServerBindingNotReused {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 摘册后的绑定不得复用：重建路径对未注册名显式失败（IllegalArgumentException 带名字）。 */
	@Test
	public void testDeregisteredServerBindingIsNotReused() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of(); // game1 已被 SM 摘除
		var bound = newUninitialized(StubSession.class);
		var addr = clientAddr(55);
		FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", bound));

		var ex = assertThrows(IllegalArgumentException.class,
				() -> FileSessionManager.resolve(agent, addr, false, false, "game1", "zeze"),
				"摘册的绑定不得复用——重建对未注册名必须显式失败");
		assertTrue(ex.getMessage().contains("game1"), "异常携带服务器名便于诊断: " + ex.getMessage());
		assertEquals(1, agent.newSessionCalls, "摘册绑定必须走重建路径（newSession 被调用）");
	}

	/** 在册绑定照常复用：注册表校验只针对摘册形态，不误伤正常续页路径。 */
	@Test
	public void testRegisteredBindingStillReused() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of("game1");
		var bound = newUninitialized(StubSession.class);
		var addr = clientAddr(56);
		FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", bound));

		assertSame(bound, FileSessionManager.resolve(agent, addr, false, false, "game1", "zeze"),
				"在册服务器的绑定照常复用");
		assertEquals(0, agent.newSessionCalls, "复用命中不得重建");
	}

	private static SocketAddress clientAddr(int lastOctet) throws Exception {
		return new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, (byte)lastOctet}), 12345);
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

	/** registry 可变（模拟 SM 增删）；newSession 对未注册名抛带名字的 IAE（对齐修复后
	 * Session 构造的显式失败形态）。 */
	private static final class StubLogAgent extends LogAgent {
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		Set<String> registry = Set.of("game1");
		int newSessionCalls;

		@Override
		public Session newSession(String serverName, String logName) {
			++newSessionCalls;
			if (!registry.contains(serverName))
				throw new IllegalArgumentException("unknown log server: " + serverName);
			try {
				return newUninitialized(StubSession.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Set<String> getLogServers() {
			return registry;
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
