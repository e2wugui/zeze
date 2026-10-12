package Zeze.log;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.LogAgent;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;

import harness.Fast;

/**
 * operateRecovering 的错误分诊直测：参数级失败（服务端对非法 containsType/空条件/
 * offsetFactor 的非零码应答，码非 LogicError）不得被判为会话级死亡——否则健康查询会话
 * 被整组关旧建新、同参数重试再失败（3N 条 RPC 抖动 + 同 IP 游标重置），最终恒
 * system error；判据只能是异常类型/结果码，不能是异常消息前缀——前缀判别把一切非零码
 * （含参数错误）都当成会话死亡，是本缺陷的根。
 * stub 形制对齐 TestSessionAllOperatePartialFailure：ReflectionFactory 不调构造器分配
 * 实例（Session/LogAgent 构造均触真实 RPC），行为由 preset 字段驱动。
 *
 * <p>@Isolated：写 FileSessionManager 静态表（每用例独立源 IP 隔离），独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestParamErrorNoSessionRebuild {

	/** 会话身份条件指纹样本（值任意，比对按值等价）。 */
	private static final String COND = "search|-1|-1|1|[error]|";
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 参数级失败不拆会话：operate 抛参数级异常（"search/browse error -1" 消息——服务端
	 * 对非法参数经异常通道应答的码）时必须原样上抛，newSession 只调用一次（不重建、不重试）。
	 * 重建出的新会话注入同一失败：同参数重试在真实服务端必然以同样方式再失败。
	 */
	@Test
	public void testParamLevelErrorDoesNotRebuildSession() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.sessionFailure = new RuntimeException("search/browse error -1");

		var addr = clientAddr(51);
		var ex = assertThrows(RuntimeException.class,
				() -> FileSessionManager.operateRecovering(agent, addr, false, false, "game1", "zeze", COND,
						this::opSearch));
		assertTrue(ex.getMessage().startsWith("search/browse error "),
				"参数级异常必须原样上抛（消息保留便于诊断）: " + ex.getMessage());
		assertEquals(1, agent.newSessionCalls, "参数级失败不得触发会话驱逐重建");
	}

	/** 网络类异常（无码、非会话级）：不重建——重建只会白白丢弃仍有效的会话与游标。 */
	@Test
	public void testNetworkErrorDoesNotRebuildSession() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.sessionFailure = new RuntimeException("connection reset");

		var addr = clientAddr(52);
		assertThrows(RuntimeException.class,
				() -> FileSessionManager.operateRecovering(agent, addr, false, false, "game1", "zeze", COND,
						this::opSearch));
		assertEquals(1, agent.newSessionCalls, "网络类瞬时失败不触发会话重建");
	}

	private BResult.Data opSearch(Object session) throws Exception {
		return ((Session)session).search(10, false, new BCondition.Data()).get(1, TimeUnit.MINUTES);
	}

	private static SocketAddress clientAddr(int lastOctet) throws Exception {
		return new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, (byte)lastOctet}), 12345);
	}

	/** 行为由 preset 字段驱动：searchFailure 非空同步抛；否则返回已完成 TCS（单条日志）。 */
	private static final class StubSession extends Session {
		@SuppressWarnings("unused")
		StubSession() {
			super(null, null, null);
		}

		RuntimeException searchFailure;
		int searchCalls;

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset, BCondition.Data condition) {
			++searchCalls;
			if (searchFailure != null)
				throw searchFailure;
			var tcs = new TaskCompletionSource<BResult.Data>();
			var data = new BResult.Data();
			data.setRemain(false);
			data.getLogs().add(new Zeze.Builtin.LogService.BLog.Data(1000, "log-line"));
			tcs.setResult(data);
			return tcs;
		}

		@Override
		public void close() {
			// 不触真实 RPC。
		}
	}

	/** newSession 计数 + 逐次返回注入 sessionFailure 的新 StubSession（真实服务端对重建
	 * 会话+同参数必然再失败同一拒绝）；getLogServers 返回注册集。 */
	private static final class StubLogAgent extends LogAgent {
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		int newSessionCalls;
		RuntimeException sessionFailure;

		@Override
		public Session newSession(String serverName, String logName) {
			++newSessionCalls;
			try {
				var stub = newUninitialized(StubSession.class);
				stub.searchFailure = sessionFailure;
				return stub;
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
