package Zeze.log;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.LogAgent;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 混合视图切换（单服绑定→全服视图请求）的 resolve 直测：复用前置校验按绑定视图分支
 * 且先于 matches/changeSession 求值时，全服请求的 serverName=null 落进
 * getLogServers().contains(null)——CHM keySet 契约必抛 NPE，且 NPE 先于 put，绑定
 * 永不被替换：全服视图（前端默认形态）对该源 IP 恒 system error，粘滞最长 2h（闲置
 * TTL），NAT 同出口相互阻断。修复后校验在 matches 之后惰性求值——视图不一致被
 * matches 短路走关旧建新，contains 只对单服视图（serverName 必非 null）求值。
 * stub 形制对齐 TestStaleServerBindingNotReused：ReflectionFactory 不调构造器分配
 * 实例（Session/SessionAll/LogAgent 构造均触真实 RPC）。
 *
 * <p>@Isolated：写 FileSessionManager 静态表（独立源 IP 隔离），独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestAllViewRequestAfterSingleServerBinding {

	/** 会话身份条件指纹样本（值任意，比对按值等价）。 */
	private static final String COND = "search|-1|-1|1|[error]|";
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 核心红点：单服绑定在库时全服视图请求必须走关旧建新（修复前 contains(null) NPE
	 * 且绑定粘滞）。 */
	@Test
	public void testAllViewRequestRebuildsOverSingleServerBinding() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of("game1"); // 不可含 null：contains(null) 抛 NPE（CHM keySet 同契约）
		var bound = newUninitialized(StubSession.class);
		var addr = clientAddr(58);
		FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", "search|-1|-1|1|[error]|", bound));

		var sessionAll = FileSessionManager.resolve(agent, addr, false, true, null, "zeze", COND);

		assertSame(agent.sessionAll, sessionAll, "全服视图请求走关旧建新（修复前 NPE）");
		assertEquals(1, agent.newSessionAllCalls, "必须重建全服会话而非复用单服绑定");
		var replaced = FileSessionManager.get(addr);
		assertTrue(replaced.all(), "绑定被替换为全服视图（修复前 NPE 先于 put，粘滞旧绑定）");
		assertEquals("zeze", replaced.logName());
	}

	/** changeSession=true 不进复用判别：同一混合视图形态同样不得 NPE（旧形态该布尔在
	 * contains 求值之后才生效，强制重建也救不了）。 */
	@Test
	public void testChangeSessionAllViewRequestAlsoRebuilds() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of("game1");
		var bound = newUninitialized(StubSession.class);
		var addr = clientAddr(59);
		FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", "search|-1|-1|1|[error]|", bound));

		var rebuilt = FileSessionManager.resolve(agent, addr, true, true, null, "zeze", COND);
		assertSame(agent.sessionAll, rebuilt, "changeSession 强制重建同样不得 NPE");
		assertTrue(FileSessionManager.get(addr).all());
	}

	/** 反方向（全服绑定+单服请求）既有安全路径回归：matches 短路视图不一致走重建，
	 * 不触全服收敛比对与 contains。 */
	@Test
	public void testSingleServerRequestOverAllViewBindingRebuilds() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of("game1");
		var bound = newUninitialized(StubSessionAll.class);
		var addr = clientAddr(60);
		FileSessionManager.put(addr, LogSessionBinding.allView("zeze", "search|-1|-1|1|[error]|", bound));

		var rebuilt = FileSessionManager.resolve(agent, addr, false, false, "game1", "zeze", COND);
		assertSame(agent.session, rebuilt, "视图切换走关旧建新");
		assertEquals(1, agent.newSessionCalls);
	}

	/** 同视图在册绑定照常复用（惰性求值不改变单服→单服的既有收敛语义）。 */
	@Test
	public void testSameViewRegisteredBindingStillReused() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of("game1");
		var bound = newUninitialized(StubSession.class);
		var addr = clientAddr(61);
		FileSessionManager.put(addr, LogSessionBinding.server("game1", "zeze", "search|-1|-1|1|[error]|", bound));

		assertSame(bound, FileSessionManager.resolve(agent, addr, false, false, "game1", "zeze", COND),
				"在册单服绑定照常复用");
		assertEquals(0, agent.newSessionCalls);
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

	/** memberNames 返回非空成员集（resolve 重建路径的 0 成员校验需要）。 */
	private static final class StubSessionAll extends SessionAll {
		@SuppressWarnings("unused")
		StubSessionAll() {
			super(null, null);
		}

		@Override
		public Set<String> memberNames() {
			return Set.of("game1");
		}

		@Override
		public void close() {
			// 不触真实 RPC。
		}
	}

	/** registry 可变（Set.of 的 contains(null) 与 CHM keySet 同为 NPE，保真红态）；
	 * newSession/newSessionAll 计数并返回桩会话。 */
	private static final class StubLogAgent extends LogAgent {
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		Set<String> registry = Set.of("game1");
		int newSessionCalls;
		int newSessionAllCalls;
		Session session;
		SessionAll sessionAll;

		@Override
		public Session newSession(String serverName, String logName) {
			++newSessionCalls;
			try {
				session = newUninitialized(StubSession.class);
				return session;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public SessionAll newSessionAll(String logName) {
			++newSessionAllCalls;
			try {
				sessionAll = newUninitialized(StubSessionAll.class);
				return sessionAll;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Set<String> getLogServers() {
			return registry;
		}
	}

	/** 不调构造器分配实例（字段全默认值）：Session/SessionAll/LogAgent 构造器都触真实 RPC。 */
	@SuppressWarnings("restriction")
	private static <T> T newUninitialized(Class<T> clazz) throws Exception {
		@SuppressWarnings("unchecked")
		Constructor<T> ctor = (Constructor<T>)sun.reflect.ReflectionFactory.getReflectionFactory()
				.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
		return ctor.newInstance();
	}
}
