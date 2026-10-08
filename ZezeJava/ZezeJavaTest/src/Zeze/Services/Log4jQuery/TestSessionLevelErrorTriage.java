package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.Search;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.log.FileSessionManager;

import harness.Fast;

/**
 * Session.checked 的结果码分诊直测：死会话（LogicError）→ SessionLevelException（调用方
 * 驱逐重建）；参数级拒绝（LogService.INVALID_ARGUMENT）→ InvalidArgumentException
 * （不拆会话直接报参数错误）；其余非零码保持原 RuntimeException 形态。判型不判消息——
 * 消息前缀判别曾把一切非零码（含参数错误）都当成会话死亡，触发无谓的整组会话拆建重试。
 * 直构 Search/Browse rpc（生成类构造不依赖网络，对齐 TestSessionResultCodeCheck）
 * + operateRecovering 的重建/不重建行为经 stub agent 驱动（对齐
 * TestSessionAllOperatePartialFailure 的 ReflectionFactory 形制）。
 *
 * <p>@Isolated：写 FileSessionManager 静态表（独立源 IP 隔离），独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestSessionLevelErrorTriage {

	/** 会话身份条件指纹样本（值任意，比对按值等价）。 */
	private static final String COND = "search|-1|-1|1|[error]|";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 死会话码（LogicError）分诊为 SessionLevelException——重建判据的唯一来源。 */
	@Test
	public void testLogicErrorMapsToSessionLevelException() {
		var rpc = new Search();
		rpc.setResultCode(Procedure.LogicError);
		var ex = assertThrows(Session.SessionLevelException.class, () -> getChecked(rpc));
		assertTrue(ex.getMessage().contains(Long.toString(Procedure.LogicError)),
				"消息保留码便于诊断: " + ex.getMessage());
	}

	/** 参数级拒绝码（INVALID_ARGUMENT）分诊为 InvalidArgumentException——不拆会话。 */
	@Test
	public void testInvalidArgumentMapsToInvalidArgumentException() {
		var rpc = new Browse();
		rpc.setResultCode(Zeze.Services.LogService.INVALID_ARGUMENT);
		var ex = assertThrows(Session.InvalidArgumentException.class, () -> getChecked(rpc));
		assertTrue(ex.getMessage().contains(Long.toString(Zeze.Services.LogService.INVALID_ARGUMENT)),
				"消息保留码便于诊断: " + ex.getMessage());
	}

	/** 其余非零码（服务端内部异常等）保持原 RuntimeException 形态，不冒充会话级。 */
	@Test
	public void testOtherCodeStaysPlainRuntimeException() {
		var rpc = new Search();
		rpc.setResultCode(Procedure.Exception);
		var ex = assertThrows(RuntimeException.class, () -> getChecked(rpc));
		assertFalse(ex instanceof Session.SessionLevelException, "非 LogicError 码不得判为会话级");
		assertFalse(ex instanceof Session.InvalidArgumentException, "非参数码不得判为参数级");
	}

	/** isSessionLevelError 只认类型：参数级与前缀消息（历史形态）都不得判为会话级。 */
	@Test
	public void testIsSessionLevelErrorByTypeNotMessage() {
		assertTrue(Session.isSessionLevelError(new Session.SessionLevelException("search/browse error -6")));
		assertFalse(Session.isSessionLevelError(new Session.InvalidArgumentException("search/browse error -100")));
		assertFalse(Session.isSessionLevelError(new RuntimeException("search/browse error -100")),
				"消息前缀本身不是判据——参数码前缀不得触发重建");
		assertFalse(Session.isSessionLevelError(new RuntimeException("connection reset")));
	}

	/** operateRecovering：会话级死亡驱逐重建重试一次，重试成功返回结果。 */
	@Test
	public void testSessionLevelErrorRebuildsAndRetriesOnce() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.sessionFailure = new Session.SessionLevelException("search/browse error -6");
		// 首会话死亡、重建会话成功：agent.sessionFailure 在首次失败后清除（模拟服务端重建后正常）。
		var addr = clientAddr(61);
		var result = FileSessionManager.operateRecovering(agent, addr, false, false, "game1", "zeze", COND,
				TestSessionLevelErrorTriage::opSearch);
		assertEquals(2, agent.newSessionCalls, "会话级死亡触发关旧建新（newSession 两次）");
		assertEquals(1, result.getLogs().size(), "重建后同参数重试一次必须返回结果");
	}

	/** operateRecovering：参数级拒绝不拆会话，异常原样上抛（重试必然再失败）。 */
	@Test
	public void testInvalidArgumentDoesNotRebuildSession() throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.sessionFailure = new Session.InvalidArgumentException("search/browse error -100");
		var addr = clientAddr(62);
		assertThrows(Session.InvalidArgumentException.class,
				() -> FileSessionManager.operateRecovering(agent, addr, false, false, "game1", "zeze", COND,
						TestSessionLevelErrorTriage::opSearch));
		assertEquals(1, agent.newSessionCalls, "参数级拒绝不得触发会话驱逐重建");
	}

	private static BResult.Data getChecked(Zeze.Net.Rpc<?, BResult.Data> rpc) {
		var inner = new TaskCompletionSource<BResult.Data>();
		inner.setResult(new BResult.Data()); // 未解码空 Result：非零码必须在 get 时抛
		return Session.checkResultCode(rpc, inner).get();
	}

	private static BResult.Data opSearch(Object session) throws Exception {
		return ((Session)session).search(10, false, new BCondition.Data()).get(1, TimeUnit.MINUTES);
	}

	private static SocketAddress clientAddr(int lastOctet) throws Exception {
		return new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, (byte)lastOctet}), 12345);
	}

	/** 首会话注入 sessionFailure；重建出的会话不注入（服务端重建后同参数正常的形态）。 */
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

	private static final class StubLogAgent extends Zeze.Services.LogAgent {
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
				// 只有首个会话注入失败：会话级死亡的重建重试在服务端重建后恢复正常。
				stub.searchFailure = newSessionCalls == 1 ? sessionFailure : null;
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
