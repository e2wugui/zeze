package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.LogAgent;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func1;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;

import harness.Fast;

/**
 * SessionAll.operate 的总时限直测：单服务器查询分支（ZokerManager 处理器）对
 * search/browse 的 future.get 有 1 分钟上界，聚合分支此前无任何外层上界——
 * 部分失败期（注册表含死条目，SM 租约未过期）单个请求的补员/发送对不可达成员
 * 逐台串行阻塞 5s×N、等待依赖各 RPC 自身的 60s 超时，占满派发线程与同 IP 在飞
 * 守卫。修复后 operate 携带总 deadline：等待按剩余时限 get、补员与发送按剩余
 * 时限跳过；到点成员按瞬时失败同构降级（部分成功照常返回），全败抛带信息的
 * TimeoutException。直测经反射驱动带时限重载（修复前该方法不存在即红，
 * 先例 TestIdleBindingSweep），stub 形制对齐 TestSessionAllOperatePartialFailure
 * （ReflectionFactory 不调构造器，字段由注入驱动）。
 */
@Fast
public class TestSessionAllOperateTotalTimeout {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 全部成员不应答：必须在总时限内有界失败（带信息的 TimeoutException），不得无限等。 */
	@Test
	public void testUnresponsiveMembersFailWithinTotalTimeout() throws Exception {
		var slowA = newStub("server-a");
		slowA.reply = pendingFuture();
		var slowB = newStub("server-b");
		slowB.reply = pendingFuture();
		var sessionAll = newSessionAll(slowA, slowB);

		var operateWithTimeout = SessionAll.class.getMethod("operate", Func1.class, long.class);
		var begin = System.nanoTime();
		var ex = assertThrows(InvocationTargetException.class,
				() -> operateWithTimeout.invoke(sessionAll, (Func1<Session, TaskCompletionSource<BResult.Data>>)this::opSearch, 150L),
				"全服等待必须有总上界（单服分支的 1 分钟口径对称）");
		var elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
		assertTrue(ex.getCause() instanceof TimeoutException,
				"全败必须以 TimeoutException 上抛（区分于普通系统错误）: " + ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("total timeout"),
				"超时异常必须带可分诊的信息（desc 承载）: " + ex.getCause().getMessage());
		assertTrue(elapsedMs < 5000, "总时限必须封顶等待（预算150ms）: " + elapsedMs + "ms");
	}

	/** 部分成员应答慢：到点成员按单台降级，快成员的部分结果照常返回（不因超时整体丢弃）。 */
	@Test
	public void testTotalTimeoutKeepsFastMemberPartialResult() throws Exception {
		var fast = newStub("server-a");
		fast.reply = okFuture(resultOf(true, "log-from-a", 1000));
		var slow = newStub("server-b");
		slow.reply = pendingFuture();
		var sessionAll = newSessionAll(fast, slow);

		var operateWithTimeout = SessionAll.class.getMethod("operate", Func1.class, long.class);
		var begin = System.nanoTime();
		var r = (BResult.Data)operateWithTimeout.invoke(sessionAll,
				(Func1<Session, TaskCompletionSource<BResult.Data>>)this::opSearch, 150L);
		var elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
		assertEquals(1, r.getLogs().size(), "超时只降级慢成员，快成员结果不得丢弃");
		assertEquals("log-from-a", r.getLogs().get(0).getLog());
		assertTrue(r.isRemain(), "慢成员未完成，remain 不得提前收敛为 false");
		assertTrue(elapsedMs < 5000, "部分降级同样受总时限封顶: " + elapsedMs + "ms");
	}

	// ---------------------------------------------------------------- helpers

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

	/** 永不完成的应答（不可达/不应答成员形态）。 */
	private static TaskCompletionSource<BResult.Data> pendingFuture() {
		return new TaskCompletionSource<BResult.Data>();
	}

	/** 行为由 preset 字段驱动：reply 即返回。 */
	private static final class StubSession extends Session {
		// 实例化走 newUninitialized（ReflectionFactory 绕过构造器），本构造仅为满足编译；
		// 一旦被误调即 NPE 暴露。
		@SuppressWarnings("unused")
		StubSession() {
			super(null, null, null);
		}

		String stubName;
		TaskCompletionSource<BResult.Data> reply;

		@Override
		public String getName() {
			return stubName;
		}

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset, BCondition.Data condition) {
			return reply;
		}

		@Override
		public void close() {
			// 不触真实 RPC。
		}
	}

	/** 注册表返回注入的成员名（补员/逐出差集为空，聚焦 operate 循环的超时语义）。 */
	private static final class StubLogAgent extends LogAgent {
		// 实例化走 newUninitialized（ReflectionFactory 绕过构造器），本构造仅为满足编译。
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
		setFinalField(sessionAll, "deliveredWatermark", new ConcurrentHashMap<String, Long>());
		setFinalField(sessionAll, "memberTimeRegression", new ConcurrentHashSet<String>());
		setFinalField(sessionAll, "memberSeekBase", new ConcurrentHashMap<String, Long>());
		setFinalField(sessionAll, "memberForceReset", new ConcurrentHashSet<String>());
		var agent = newUninitialized(StubLogAgent.class);
		agent.servers = Set.copyOf(alls.keySet());
		setFinalField(sessionAll, "agent", agent);
		return sessionAll;
	}

	private static void setFinalField(Object obj, String name, Object value) throws Exception {
		Field field = SessionAll.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}

	/** 不调构造器分配实例（字段全默认值）：SessionAll/Session/LogAgent 的构造器都触真实 RPC。 */
	@SuppressWarnings("restriction")
	private static <T> T newUninitialized(Class<T> clazz) throws Exception {
		@SuppressWarnings("unchecked")
		Constructor<T> ctor = (Constructor<T>)sun.reflect.ReflectionFactory.getReflectionFactory()
				.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
		return ctor.newInstance();
	}
}
