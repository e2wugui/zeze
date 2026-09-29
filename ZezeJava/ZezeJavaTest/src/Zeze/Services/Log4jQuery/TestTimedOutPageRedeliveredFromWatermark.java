package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BResult;
import Zeze.Net.RpcTimeoutException;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;

import harness.Fast;

/**
 * 翻页RPC超时页的水位续扫回归：页协议是"游标推进+应答"的至多一次语义——单页服务端
 * 处理超60s（页预算100k条/256MB+seek内部无界扫描允许分钟级）时客户端future超时、
 * 迟到应答因rpc上下文已摘除被丢弃，服务端游标已越过该页；会话未死不触发
 * renewDeadMembers水位续扫，下次operate从游标续读"下一页"，丢失页窗口在聚合结果
 * 中永久缺失且仅有一次warn。修复：operate对非会话级、非参数级的成员失败记录续扫
 * ——有已投递水位则memberSeekBase=水位（下页从水位重定位重发丢失页，重复收敛为
 * 水位边界同时间少量条目）；无水位（首页未投递）置强制reset标记（服务端重定位到
 * 查询下界重发首页，无已投递即无重复）。参数级失败是确定性错误不重定位（服务端
 * 入口校验拒绝、游标未动，从游标续读即正确）。
 */
@Fast
public class TestTimedOutPageRedeliveredFromWatermark {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 已投递页之后的超时页：从水位重定位重发，聚合不缺页（重复=水位同时间条目）。 */
	@Test
	public void testTimedOutPageRedeliveredFromDeliveredWatermark() throws Exception {
		// 服务端日志time=1000..1011共12条，页大小5：page1=1000..1004，page2=1005..1009，page3=1010..1011。
		var stub = pagingStub("server-a", 1000, 12);
		var sessionAll = newSessionAll(stub);

		// 页1正常投递：水位=1004。
		var r1 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1000L, 1001L, 1002L, 1003L, 1004L), timesOf(r1));

		// 页2超时（服务端游标已越过1005..1009、应答丢弃、水位不动）：单成员全失败operate抛。
		stub.timeoutNextPage = true;
		assertThrows(Exception.class, () -> sessionAll.search(5, false, condition(0L)),
				"超时页不投递，单成员全失败必须抛（与查完无匹配不可混淆）");

		// 修复前：下次operate重发同beginTime被服务端去重哨兵短路，从游标（已越过丢失页）
		// 续读1010..1011——1005..1009永久缺失无信号。修复后：从水位1004重定位，
		// 重发丢失页（1004同时间重复+1005..1008）。
		var r3 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1004L, 1005L, 1006L, 1007L, 1008L), timesOf(r3),
				"超时页必须从已投递水位重定位重发，不得从服务端游标静默跳页");

		// 余页续读完（固定基点短路，从游标续读）。
		var r4 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1009L, 1010L, 1011L), timesOf(r4), "余页续读");

		// 聚合完整性：全部12条均可达（1004为水位边界重复）。
		var delivered = new ArrayList<Long>();
		for (var r : List.of(r1, r3, r4))
			for (var log : r.getLogs())
				delivered.add(log.getTime());
		assertEquals(Set.of(1000L, 1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L, 1011L),
				Set.copyOf(delivered), "聚合结果不得缺页（唯一允许的重复=水位同时间条目）");
	}

	/** 首页超时（无已投递水位）：强制reset重定位到查询下界重发首页，无已投递即无重复。 */
	@Test
	public void testTimedOutFirstPageForcedResetRedelivers() throws Exception {
		var stub = pagingStub("server-b", 2000, 10);
		var sessionAll = newSessionAll(stub);

		stub.timeoutNextPage = true;
		assertThrows(Exception.class, () -> sessionAll.search(5, false, condition(0L)),
				"首页超时不投递，全失败必须抛");

		// 修复前：重发同beginTime被哨兵短路，从游标续读2005..2009——首页2000..2004永久缺失。
		// 修复后：无水位成员下一页强制reset，服务端重定位到查询下界重发首页。
		var r2 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(2000L, 2001L, 2002L, 2003L, 2004L), timesOf(r2),
				"首页超时后必须强制reset重定位重发，不得静默跳过首页");

		var r3 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(2005L, 2006L, 2007L, 2008L, 2009L), timesOf(r3), "续页读完");
	}

	/** 参数级失败是确定性错误（服务端入口校验拒绝、游标未动）：不重定位，从游标续读。 */
	@Test
	public void testArgumentFailureDoesNotReseek() throws Exception {
		var stub = pagingStub("server-c", 3000, 10);
		var sessionAll = newSessionAll(stub);

		sessionAll.search(5, false, condition(0L)); // 页1投递，水位3004
		stub.failNextPageWithArgument = true;
		assertThrows(Session.InvalidArgumentException.class,
				() -> sessionAll.search(5, false, condition(0L)));
		stub.failNextPageWithArgument = false;

		// 参数错误重定位无益（重发必然再失败）：游标未动，修好参数后从游标续读下一页。
		var r3 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(3005L, 3006L, 3007L, 3008L, 3009L), timesOf(r3),
				"参数级失败不触发重定位，从游标续读即正确");
	}

	private static BCondition.Data condition(long beginTime) {
		var condition = new BCondition.Data();
		condition.setBeginTime(beginTime);
		condition.setEndTime(-1);
		condition.setContainsType(BCondition.ContainsNone);
		condition.getWords().add("never-match");
		return condition;
	}

	private static List<Long> timesOf(BResult.Data r) {
		var times = new ArrayList<Long>();
		for (var log : r.getLogs())
			times.add(log.getTime());
		return times;
	}

	/**
	 * 服务端语义复刻的stub：日志按time升序、游标顺序消费；reset归零游标+失效去重哨兵，
	 * beginTime变化重定位到首条>=beginTime；超时页"被服务端处理但应答丢弃"——游标照常
	 * 推进、future以RpcTimeoutException失败（Rpc.schedule超时摘上下文、迟到应答
	 * onRpcLostContext丢弃的形态）。
	 */
	private static final class PagingStubSession extends Session {
		@SuppressWarnings("unused")
		PagingStubSession() {
			super(null, null, null);
		}

		String stubName;
		long firstTime;
		int logCount;
		int cursor;
		Long committedBeginTime; // 服务端beginTime去重哨兵
		boolean timeoutNextPage;
		boolean failNextPageWithArgument;

		@Override
		public String getName() {
			return stubName;
		}

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset, BCondition.Data condition) {
			if (failNextPageWithArgument)
				throw new Session.InvalidArgumentException("search/browse error -100");
			if (reset) {
				cursor = 0;
				committedBeginTime = null;
			}
			var beginTime = condition.getBeginTime();
			if (committedBeginTime == null || committedBeginTime != beginTime) {
				cursor = firstIndexAtLeast(beginTime); // resetWalker+seek
				committedBeginTime = beginTime;
			}
			var end = Math.min(cursor + limit, logCount);
			var data = new BResult.Data();
			for (var i = cursor; i < end; ++i)
				data.getLogs().add(new BLog.Data(firstTime + i, "log-" + i));
			cursor = end; // 游标推进（至多一次语义：应答丢弃页也被越过）
			data.setRemain(cursor < logCount);
			if (timeoutNextPage) {
				timeoutNextPage = false;
				var tcs = new TaskCompletionSource<BResult.Data>();
				tcs.setException(RpcTimeoutException.getInstance());
				return tcs;
			}
			var future = new TaskCompletionSource<BResult.Data>();
			future.setResult(data);
			return future;
		}

		private int firstIndexAtLeast(long beginTime) {
			for (var i = 0; i < logCount; ++i)
				if (firstTime + i >= beginTime)
					return i;
			return logCount;
		}

		@Override
		public void close() {
			// 不触真实RPC。
		}
	}

	private static PagingStubSession pagingStub(String name, long firstTime, int logCount) throws Exception {
		var stub = newUninitialized(PagingStubSession.class);
		stub.stubName = name;
		stub.firstTime = firstTime;
		stub.logCount = logCount;
		return stub;
	}

	/** 注册表=会话成员（补员/逐出差集为空），同TestSessionAllOperatePartialFailure形制。 */
	private static final class StubLogAgent extends Zeze.Services.LogAgent {
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

	private static SessionAll newSessionAll(Session... sessions) throws Exception {
		var sessionAll = newUninitialized(SessionAll.class);
		var alls = new ConcurrentHashMap<String, Session>();
		for (var session : sessions)
			alls.put(session.getName(), session);
		setField(sessionAll, "alls", alls);
		setField(sessionAll, "finishedSession", new ConcurrentHashSet<String>());
		setField(sessionAll, "memberRetryBackoff", new ConcurrentHashMap<String, Long>());
		setField(sessionAll, "deliveredWatermark", new ConcurrentHashMap<String, Long>());
		setField(sessionAll, "memberSeekBase", new ConcurrentHashMap<String, Long>());
		// 修复新增字段（红阶段不存在则跳过，行为断言承载红绿）。
		setFieldIfPresent(sessionAll, "memberForceReset", new ConcurrentHashSet<String>());
		var agent = newUninitialized(StubLogAgent.class);
		agent.servers = Set.copyOf(alls.keySet());
		setField(sessionAll, "agent", agent);
		return sessionAll;
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field field = SessionAll.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}

	private static void setFieldIfPresent(Object obj, String name, Object value) throws Exception {
		try {
			setField(obj, name, value);
		} catch (NoSuchFieldException e) {
			// 修复前字段不存在：跳过（红由行为断言承载）。
		}
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
