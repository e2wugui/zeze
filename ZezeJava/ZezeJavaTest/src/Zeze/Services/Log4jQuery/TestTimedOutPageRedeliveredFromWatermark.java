package Zeze.Services.Log4jQuery;

import harness.Extra;
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
 * FND34 log4jquery-01 补同值窗口回归：续扫基点==水位（重建/重入成员的首页丢失）
 * 或水位==原始beginTime（同值页边界）时覆写值==服务端去重哨兵、短路命中，唯有
 * 强制reset能重定位——markTransientLoss 有水位也置 memberForceReset。
 */
@Fast
@Extra
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

	/**
	 * 重建成员首页超时（续扫基点==水位）：死亡重建把基点置为当时的已投递水位，重建
	 * 会话首页携带 beginTime=基点提交服务端去重哨兵——该页恰逢超时则水位未推进仍
	 * ==基点==哨兵，markTransientLoss 只写 seekBase 对哨兵是 no-op：下一页短路命中、
	 * 从已越过丢失页的游标续读，丢失页窗口永久缺失（FND34 log4jquery-01 形态1）。
	 * 修复：该形态同时置强制 reset——reset+seek(水位) 重定位重发丢失页，重复收敛为
	 * 水位边界同时间条目；重发后的余页从哨兵短路续读，不回退游标造成整段重复。
	 */
	@Test
	public void testRenewedMemberFirstPageTimeoutRedeliversFromBase() throws Exception {
		// A（1000..1014）与 B（5000..5014）双成员：A 死亡重建降级期 B 保底投递（部分失败
		// 才触发 renewDeadMembers，单成员全失败 operate 即抛不重建）。newUninitialized
		// 不跑字段初始化器，final 表在此显式初始化。
		var agent = newUninitialized(StubLogAgent.class);
		setField(agent, "renewConf", new ConcurrentHashMap<String, long[]>());
		setField(agent, "lastRenewed", new ConcurrentHashMap<String, Session>());
		agent.renewConf.put("server-renew-a", new long[] {1000, 15});
		agent.renewConf.put("server-renew-b", new long[] {5000, 15});
		var a = pagingStub("server-renew-a", 1000, 15);
		var b = pagingStub("server-renew-b", 5000, 15);
		var sessionAll = newSessionAll(agent, a, b);

		// 页1：A 1000..1004、B 5000..5004 投递，水位 A=1004、B=5004。
		var r1 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1000L, 1001L, 1002L, 1003L, 1004L,
				5000L, 5001L, 5002L, 5003L, 5004L), timesOf(r1));

		// A 会话级死亡（服务端闲置回收）：B 投递 5005..5009 部分失败降级，
		// renewDeadMembers 重建 A'（续扫基点=水位1004，新会话哨兵为空）。
		a.failNextPageWithSessionLevel = true;
		var r2 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(5005L, 5006L, 5007L, 5008L, 5009L), timesOf(r2), "A 死亡降级期 B 照常投递");
		var renewed = (PagingStubSession)agent.lastRenewed.get("server-renew-a");

		// 重建成员首页超时：哨兵提交 1004、游标推进到 1009，水位不动（仍=1004=基点）。
		renewed.timeoutNextPage = true;
		var r3 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(5010L, 5011L, 5012L, 5013L, 5014L), timesOf(r3), "A' 首页超时降级期 B 照常投递");

		// 修复前：下一页 beginTime=1004==哨兵短路，从已越过丢失页的游标续读 1009..1013
		// ——A 的 1005..1008 永久缺失。修复后：强制 reset 重定位到首条>=1004，重发
		// 1004..1008（1004 为水位边界重复）。
		var r4 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1004L, 1005L, 1006L, 1007L, 1008L), aTimesOf(r4),
				"重建首页超时必须重定位重发，不得从游标静默跳过丢失页窗口");

		// 重发后的余页从哨兵短路续读（哨兵=1004，beginTime 覆写同值）：不回退游标
		// 造成已重发段的二次重复；聚合完整性 A 侧 1000..1014 全覆盖（唯一重复=1004）。
		var r5 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1009L, 1010L, 1011L, 1012L, 1013L), aTimesOf(r5),
				"重定位重发后的余页从游标续读，不得整段重复");
		var r6 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1014L), aTimesOf(r6));
		var delivered = new ArrayList<Long>();
		for (var r : List.of(r1, r4, r5, r6))
			for (var log : r.getLogs())
				if (log.getTime() < 5000)
					delivered.add(log.getTime());
		assertEquals(List.of(1000L, 1001L, 1002L, 1003L, 1004L, 1004L, 1005L, 1006L, 1007L, 1008L,
				1009L, 1010L, 1011L, 1012L, 1013L, 1014L), delivered,
				"A 侧聚合：全覆盖且唯一重复=水位边界同时间条目 1004");
	}

	/**
	 * 水位==原始 beginTime 的同值页边界（FND34 log4jquery-01 形态2）：秒级精度日志
	 * 整秒成千上万条跨页常态，最后成功页全部日志同时间时水位 merge 后仍==beginTime
	 * ——此后任一页瞬时失败，seekBase 覆写值==服务端哨兵（首请求提交的原始
	 * beginTime）短路命中，丢失页窗口缺失。该形态无既有基点（put 返回 null），
	 * "基点未推进才置 reset"判不住，须无条件强制重定位。全页同时间使重发从文件头
	 * 起（首条>=水位），重复=整页同时间条目（病理上界），内容完整性恢复。
	 */
	@Test
	public void testSameTimeWatermarkEqualsBeginTimeTimeoutRedelivers() throws Exception {
		var stub = pagingStub("server-sametime", 7000, 15);
		stub.sameTime = true; // 全部日志同时间 7000（整秒日志的现实形态）
		var sessionAll = newSessionAll(stub);

		// 页1投递 log-0..4：水位=7000==beginTime（同值短路窗口成立）。
		var r1 = sessionAll.search(5, false, condition(7000L));
		assertEquals(List.of("log-0", "log-1", "log-2", "log-3", "log-4"), logsOf(r1));

		// 页2超时：游标推进到 10、水位仍 7000；无既有基点。
		stub.timeoutNextPage = true;
		assertThrows(Exception.class, () -> sessionAll.search(5, false, condition(7000L)),
				"单成员全失败必须抛");

		// 修复前：beginTime=7000==哨兵短路，从游标续读 log-10..14——log-5..9 永久缺失。
		// 修复后：强制 reset 重定位到首条>=7000=文件头，重发 log-0..4（整页同时间重复）。
		var r3 = sessionAll.search(5, false, condition(7000L));
		assertEquals(List.of("log-0", "log-1", "log-2", "log-3", "log-4"), logsOf(r3),
				"同值窗口必须强制重定位重发，不得从游标静默跳过丢失页窗口");

		// 后续页可达丢失窗内容（log-5..9、log-10..14），聚合内容完整。
		var r4 = sessionAll.search(5, false, condition(7000L));
		assertEquals(List.of("log-5", "log-6", "log-7", "log-8", "log-9"), logsOf(r4));
		var r5 = sessionAll.search(5, false, condition(7000L));
		assertEquals(List.of("log-10", "log-11", "log-12", "log-13", "log-14"), logsOf(r5));
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

	/** A 侧时间线（<5000）过滤：双成员归并结果中断言重建成员的投递序列。 */
	private static List<Long> aTimesOf(BResult.Data r) {
		var times = new ArrayList<Long>();
		for (var log : r.getLogs())
			if (log.getTime() < 5000)
				times.add(log.getTime());
		return times;
	}

	/** 日志内容串（同时间页形态的行级区分：times 全同，断言按 log-i 内容）。 */
	private static List<String> logsOf(BResult.Data r) {
		var logs = new ArrayList<String>();
		for (var log : r.getLogs())
			logs.add(log.getLog());
		return logs;
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
		boolean failNextPageWithSessionLevel; // 会话级死亡（服务端闲置回收回 LogicError 形态）
		boolean sameTime; // 全部日志同时间（秒级精度整秒日志的现实形态）

		@Override
		public String getName() {
			return stubName;
		}

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset, BCondition.Data condition) {
			if (failNextPageWithArgument)
				throw new Session.InvalidArgumentException("search/browse error -100");
			if (failNextPageWithSessionLevel) {
				failNextPageWithSessionLevel = false;
				throw new Session.SessionLevelException("search/browse error -6");
			}
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
				data.getLogs().add(new BLog.Data(sameTime ? firstTime : firstTime + i, "log-" + i));
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

	/** 注册表=会话成员（补员/逐出差集为空），同TestSessionAllOperatePartialFailure形制；
	 * renewConf 配置的成员可经 newSession 重建（renewDeadMembers 路径），每次产出新
	 * stub 记入 lastRenewed（测试对重建会话摆超时/死亡形态）。 */
	private static final class StubLogAgent extends Zeze.Services.LogAgent {
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		Set<String> servers = Set.of();
		final ConcurrentHashMap<String, long[]> renewConf = new ConcurrentHashMap<>();
		final ConcurrentHashMap<String, Session> lastRenewed = new ConcurrentHashMap<>();

		@Override
		public Set<String> getLogServers() {
			return servers;
		}

		@Override
		public Session newSession(String serverName, String logName) {
			var conf = renewConf.get(serverName);
			if (conf == null)
				throw new IllegalArgumentException("unknown log server: " + serverName);
			try {
				var stub = pagingStub(serverName, conf[0], (int)conf[1]);
				lastRenewed.put(serverName, stub);
				return stub;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static SessionAll newSessionAll(Session... sessions) throws Exception {
		return newSessionAll(newUninitialized(StubLogAgent.class), sessions);
	}

	private static SessionAll newSessionAll(StubLogAgent agent, Session... sessions) throws Exception {
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
		setFieldIfPresent(sessionAll, "memberTimeRegression", new ConcurrentHashSet<String>());
		agent.servers = Set.copyOf(alls.keySet());
		setField(sessionAll, "agent", agent);
		return sessionAll;
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field field = obj.getClass().getDeclaredField(name);
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
