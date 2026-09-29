package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.LogAgent;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;

import harness.Fast;

/**
 * GD-D06回归：SessionAll.operate任一台future.get()异常即整体抛，已成功台结果被丢弃，
 * 与构造期"单台降级"契约不闭合。修复后逐台catch收集（首异常+addSuppressed），部分失败
 * 降级返回已有结果+warn（失败台不标记finishedSession，下次operate自然重试它），全失败仍抛聚合异常。
 * SessionAll/Session构造均触真实RPC，按TestArchOnlineSpec先例以ReflectionFactory不调构造器分配
 * 实例（字段默认值），再反射注入依赖，直测operate循环。
 */
@Fast
public class TestSessionAllOperatePartialFailure {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 收集期单台失败：其余台结果正常返回；失败台未被标记，下次operate重试它；
	 * 成功台（Remain=false）被标记，下次operate跳过（不再发送）。
	 */
	@Test
	public void testFutureFailureKeepsOthersAndRetries() throws Exception {
		var okA = newStub("server-a");
		okA.reply = okFuture(resultOf(false, "log-from-a", 1000));
		var failB = newStub("server-b");
		failB.reply = failFuture(new RuntimeException("rpc-timeout-b"));
		var sessionAll = newSessionAll(okA, failB);

		var r1 = sessionAll.operate(this::opSearch);
		// 修复前：failB的future.get()直接外抛，server-a的已成功结果被丢弃。
		assertEquals(1, r1.getLogs().size(), "单台失败不应牺牲其余台结果");
		assertEquals("log-from-a", r1.getLogs().get(0).getLog());
		assertEquals(1, okA.searchCalls);
		assertEquals(1, failB.searchCalls);

		// 失败台恢复后：下次operate只重试失败台（成功台已标记finishedSession跳过）。
		failB.reply = okFuture(resultOf(false, "log-from-b", 2000));
		var r2 = sessionAll.operate(this::opSearch);
		assertEquals(1, okA.searchCalls, "Remain=false的成功台下次operate应跳过");
		assertEquals(2, failB.searchCalls, "失败台未被标记，下次operate应重试");
		assertEquals(1, r2.getLogs().size());
		assertEquals("log-from-b", r2.getLogs().get(0).getLog());
	}

	/**
	 * 发送阶段失败（op.call同步抛，如连接已死）：与future.get()失败同构按单台降级。
	 */
	@Test
	public void testSendPhaseFailureDegrades() throws Exception {
		var okA = newStub("server-a");
		okA.reply = okFuture(resultOf(true, "log-from-a", 1000));
		var deadB = newStub("server-b");
		deadB.sendFail = new IllegalStateException("send-fail-b");
		var sessionAll = newSessionAll(okA, deadB);

		var r = sessionAll.operate(this::opSearch);
		assertEquals(1, r.getLogs().size(), "发送期单台失败应降级返回其余台结果");
		assertEquals("log-from-a", r.getLogs().get(0).getLog());
	}

	/**
	 * 全部失败必须抛（空结果在调用方语义是"查完无匹配"，不可与查询失败混淆），
	 * 其余失败以suppressed聚合在首异常上。
	 */
	@Test
	public void testAllFailThrowsAggregated() throws Exception {
		var failA = newStub("server-a");
		failA.reply = failFuture(new RuntimeException("fail-a"));
		var failB = newStub("server-b");
		failB.reply = failFuture(new RuntimeException("fail-b"));
		var sessionAll = newSessionAll(failA, failB);

		var ex = assertThrows(Exception.class, () -> sessionAll.operate(this::opSearch));
		assertEquals(1, ex.getSuppressed().length, "其余台的失败应聚合为suppressed");
	}

	private TaskCompletionSource<BResult.Data> opSearch(Session session) {
		return session.search(10, false, new BCondition.Data());
	}

	private static BResult.Data resultOf(boolean remain, String log, long time) {
		var data = new BResult.Data();
		data.setRemain(remain);
		data.getLogs().add(new BLog.Data(time, log));
		return data;
	}

	private static TaskCompletionSource<BResult.Data> okFuture(BResult.Data data) {
		var tcs = new TaskCompletionSource<BResult.Data>();
		tcs.setResult(data);
		return tcs;
	}

	private static TaskCompletionSource<BResult.Data> failFuture(Throwable e) {
		var tcs = new TaskCompletionSource<BResult.Data>();
		tcs.setException(e);
		return tcs;
	}

	/** 行为由preset字段驱动：reply非空返回之；sendFail非空同步抛（发送期失败形态）。 */
	private static final class StubSession extends Session {
		// 实例化走newUninitialized（ReflectionFactory绕过构造器），本构造仅为满足编译；
		// 一旦被误调即NPE暴露。
		@SuppressWarnings("unused")
		StubSession() {
			super(null, null, null);
		}

		String stubName;
		TaskCompletionSource<BResult.Data> reply;
		RuntimeException sendFail;
		int searchCalls;

		@Override
		public String getName() {
			return stubName;
		}

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset, BCondition.Data condition) {
			++searchCalls;
			if (null != sendFail)
				throw sendFail;
			return reply;
		}

		@Override
		public void close() {
			// 不触真实RPC。
		}
	}

	/**
	 * operate入口的成员集维护（reconcileMissingMembers/evictUnregisteredMembers）读
	 * agent.getLogServers()：注册表返回注入的stub成员名，使补员差集与逐出差集都为空、
	 * 无newSession/evict真实路径，保持本测试的operate循环部分失败降级语义。
	 */
	private static final class StubLogAgent extends LogAgent {
		// 实例化走newUninitialized（ReflectionFactory绕过构造器），本构造仅为满足编译；
		// 一旦被误调即触真实初始化暴露。
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		Set<String> servers = Set.of();

		@Override
		public Set<String> getLogServers() {
			return servers;
		}
	}

	private static StubSession newStub(String name) throws Exception {
		var stub = newUninitialized(StubSession.class);
		stub.stubName = name;
		return stub;
	}

	private static SessionAll newSessionAll(Session... sessions) throws Exception {
		var sessionAll = newUninitialized(SessionAll.class);
		var alls = new ConcurrentHashMap<String, Session>();
		for (var session : sessions)
			alls.put(session.getName(), session);
		setFinalField(sessionAll, "alls", alls);
		setFinalField(sessionAll, "finishedSession", new ConcurrentHashSet<String>());
		setFinalField(sessionAll, "memberRetryBackoff", new ConcurrentHashMap<String, Long>());
		// 成员级水位/续扫基点表（重建续扫用）：注入空表，operate的成功路径会推进水位。
		setFinalField(sessionAll, "deliveredWatermark", new ConcurrentHashMap<String, Long>());
		setFinalField(sessionAll, "memberSeekBase", new ConcurrentHashMap<String, Long>());
		var agent = newUninitialized(StubLogAgent.class);
		agent.servers = Set.copyOf(alls.keySet()); // 注册表=会话成员：补员/逐出差集为空
		setFinalField(sessionAll, "agent", agent);
		return sessionAll;
	}

	private static void setFinalField(Object obj, String name, Object value) throws Exception {
		Field field = SessionAll.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}

	/** 不调构造器分配实例（字段全默认值）：SessionAll/Session的构造器都触真实RPC。 */
	@SuppressWarnings("restriction")
	private static <T> T newUninitialized(Class<T> clazz) throws Exception {
		@SuppressWarnings("unchecked")
		Constructor<T> ctor = (Constructor<T>)sun.reflect.ReflectionFactory.getReflectionFactory()
				.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
		return ctor.newInstance();
	}
}
