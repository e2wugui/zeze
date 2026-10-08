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
 * 非单调扫描流（轮转序）上瞬时失败的水位续扫回归（FND35 log4jquery-02）。
 * 列表不变式是轮转序非内容时间序（FND29）：rotate 内容时间可晚于 active（时钟步进
 * 回拨后轮转），扫描流时间先升后降。水位续扫（deliveredWatermark=max time）的
 * "不丢"论证隐含扫描流时间单调——非单调形态下水位=T2（rotate）而未投递段在 active
 * （time=T1&lt;T2），瞬时失败强制 reset 重定位 beginTime=水位时，服务端 seek(T2)+
 * 窗口下界覆写把 active 未投递段逐条滤除，聚合静默缺窗且 remain 正常收敛不可观测。
 * 修复后：成员已投递页序列出现时间回退（后页 time &lt; 先前水位）即判非单调，续扫
 * 保守回落——清除基点（下发原始 beginTime）+强制 reset 整窗重扫：重复可观测、可
 * 幂等去重，丢失不可观测，宁重复不丢失；单调成员保持水位续扫（既有紧凑语义不变）。
 */
@Fast
@Extra
public class TestTransientLossOnNonMonotonicScanKeepsWindow {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 主链（案卷形态）：rotate 段 time=5000..5004、active 段 time=1000..1009（T1&lt;T2）。
	 * 页1 投递 rotate（水位=5004）、页2 投递 active 前段（页序时间回退=非单调观测点）、
	 * 页3（active 尾段 1005..1009）超时。修复前：从水位 5004 重定位，rotate 边界条重投后
	 * active 全段被窗口下界滤除、remain 收敛 false——尾段永久缺失；修复后：保守回落原始
	 * 条件整窗重扫，active 尾段可达（重复=已投递段重投，可去重）。
	 */
	@Test
	public void testTimeoutDuringOlderGenerationResumesConservatively() throws Exception {
		var stub = nonMonotonicStub("server-reg", 5000L, 5, 1000L, 10);
		var sessionAll = newSessionAll(stub);

		// 页1：rotate 5000..5004 投递，水位=5004（尚无回退观测）。
		var r1 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(5000L, 5001L, 5002L, 5003L, 5004L), timesOf(r1));

		// 页2：active 1000..1004 投递——后页时间低于先前水位（5004），非单调观测点。
		var r2 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1000L, 1001L, 1002L, 1003L, 1004L), timesOf(r2));

		// 页3（1005..1009）超时：游标被越过、水位不动（仍=5004），单成员全失败 operate 抛。
		stub.timeoutNextPage = true;
		assertThrows(Exception.class, () -> sessionAll.search(5, false, condition(0L)),
				"超时页不投递，单成员全失败必须抛");

		// 修复前：重定位 beginTime=5004——边界条 5004 重投后 active 全段滤除、查完收尾，
		// 1005..1009 永久缺失。修复后：非单调成员保守整窗重扫，翻页到查完。
		var delivered = new ArrayList<Long>();
		for (var r : List.of(r1, r2))
			delivered.addAll(timesOf(r));
		for (var remain = true; remain; ) {
			var r = sessionAll.search(5, false, condition(0L));
			delivered.addAll(timesOf(r));
			remain = r.isRemain();
		}
		assertEquals(Set.of(1000L, 1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L,
						5000L, 5001L, 5002L, 5003L, 5004L),
				Set.copyOf(delivered),
				"聚合不得缺窗：active 尾段（1005..1009）必须可达（修复前被水位重定位滤除静默缺失）");
	}

	/**
	 * 对照组：单调扫描流（既有 FND31/FND34 语义）不受保守回落影响——瞬时失败仍从水位
	 * 紧凑续扫，重复仅水位边界同时间条目，不整窗重扫。
	 */
	@Test
	public void testMonotonicMemberKeepsTightWatermarkResume() throws Exception {
		var stub = nonMonotonicStub("server-mono", 1000L, 15, 0L, 0); // 单调流：1000..1014
		var sessionAll = newSessionAll(stub);

		var r1 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1000L, 1001L, 1002L, 1003L, 1004L), timesOf(r1));

		stub.timeoutNextPage = true;
		assertThrows(Exception.class, () -> sessionAll.search(5, false, condition(0L)));

		// 水位=1004 紧凑续扫：重发 1004..1008（1004 为水位边界重复），不从头整窗重扫。
		var r3 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1004L, 1005L, 1006L, 1007L, 1008L), timesOf(r3),
				"单调成员保持水位紧凑续扫（修复不得回退 FND31/FND34 语义）");
		var r4 = sessionAll.search(5, false, condition(0L));
		assertEquals(List.of(1009L, 1010L, 1011L, 1012L, 1013L), timesOf(r4), "余页续读");
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
	 * 服务端语义复刻stub（轮转序非单调形态）：扫描流=rotate段（time=T2系，共rotateCount条）
	 * 后接active段（time=T1系，共activeCount条，T1&lt;T2）。reset归零游标+失效去重哨兵，
	 * beginTime变化重定位到首条>=beginTime；窗口下界与定位同源（beginTime覆写即滤除
	 * 其后所有time&lt;beginTime的日志——服务端seek的detailSeek跳过+查询循环下界过滤的
	 * 合成语义）；超时页游标照常推进、future以RpcTimeoutException失败。
	 */
	private static final class NonMonotonicStubSession extends Session {
		@SuppressWarnings("unused")
		NonMonotonicStubSession() {
			super(null, null, null);
		}

		String stubName;
		long rotateFirstTime;
		int rotateCount;
		long activeFirstTime;
		int activeCount;
		int cursor;
		Long committedBeginTime; // 服务端beginTime去重哨兵
		boolean timeoutNextPage;

		int total() {
			return rotateCount + activeCount;
		}

		long timeOf(int i) {
			return i < rotateCount ? rotateFirstTime + i : activeFirstTime + (i - rotateCount);
		}

		@Override
		public String getName() {
			return stubName;
		}

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset, BCondition.Data condition) {
			if (reset) {
				cursor = 0;
				committedBeginTime = null;
			}
			var beginTime = condition.getBeginTime();
			if (committedBeginTime == null || committedBeginTime != beginTime) {
				cursor = firstIndexAtLeast(beginTime); // resetWalker+seek（窗口下界同源滤除）
				committedBeginTime = beginTime;
			}
			var data = new BResult.Data();
			var served = 0;
			while (cursor < total() && served < limit) {
				var t = timeOf(cursor);
				if (t >= beginTime) {
					data.getLogs().add(new BLog.Data(t, "log-" + cursor));
					++served;
				}
				++cursor; // 窗外条照常越过（查询循环下界过滤后游标推进）
			}
			data.setRemain(cursor < total());
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
			for (var i = 0; i < total(); ++i)
				if (timeOf(i) >= beginTime)
					return i;
			return total();
		}

		@Override
		public void close() {
			// 不触真实RPC。
		}
	}

	private static NonMonotonicStubSession nonMonotonicStub(String name, long rotateFirstTime, int rotateCount,
															long activeFirstTime, int activeCount) throws Exception {
		var stub = newUninitialized(NonMonotonicStubSession.class);
		stub.stubName = name;
		stub.rotateFirstTime = rotateFirstTime;
		stub.rotateCount = rotateCount;
		stub.activeFirstTime = activeFirstTime;
		stub.activeCount = activeCount;
		return stub;
	}

	/** 注册表=会话成员（补员/逐出差集为空），同TestTimedOutPageRedeliveredFromWatermark形制。 */
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

		@Override
		public Session newSession(String serverName, String logName) {
			throw new IllegalArgumentException("unknown log server: " + serverName);
		}
	}

	private static SessionAll newSessionAll(Session session) throws Exception {
		var sessionAll = newUninitialized(SessionAll.class);
		var alls = new ConcurrentHashMap<String, Session>();
		alls.put(session.getName(), session);
		setField(sessionAll, "alls", alls);
		setField(sessionAll, "finishedSession", new ConcurrentHashSet<String>());
		setField(sessionAll, "memberRetryBackoff", new ConcurrentHashMap<String, Long>());
		setField(sessionAll, "deliveredWatermark", new ConcurrentHashMap<String, Long>());
		setField(sessionAll, "memberSeekBase", new ConcurrentHashMap<String, Long>());
		setFieldIfPresent(sessionAll, "memberForceReset", new ConcurrentHashSet<String>());
		// 修复新增字段（红阶段不存在则跳过，行为断言承载红绿）。
		setFieldIfPresent(sessionAll, "memberTimeRegression", new ConcurrentHashSet<String>());
		var agent = newUninitialized(StubLogAgent.class);
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
