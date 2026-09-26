package Onz;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import harness.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND19 GC-D04 回归（B1+C）：
 * <b>B1（红测）</b>：cleanupTimeoutSagas 原先按 startTime（构造时刻）计时——补偿失败
 * 放回 sagas 等待重发的上下文，会在构造时刻超 TTL 时被清，补偿重试链被 TTL 掐断，
 * 重发只得 eSagaNotFound，补偿永久丢失。修复：计时基准改最后活动时间（构造初始化，
 * 补偿失败放回时刷新）——补偿重试链推进期间上下文不被清；无活动超龄仍被兜底回收。
 * <b>C（红测）</b>：redo 对 rollback 决策（cancel=true）的 eSagaNotFound 原先一刀切
 * 静默忽略并删记录——超 TTL 预算的 NotFound（恶性成因：参与方上下文已被清理，补偿
 * 丢失）无任何协调者侧信号。修复：记录年龄超预算（对齐 Onz.eDefaultSagaContext
 * TimeoutMs）的升格 error（按 tid 去重，对齐 hangWarnedTids）并保留决策记录（人工
 * 对账需要记录在场）；年龄内的 NotFound 与 commit 决策（cancel=false 的 end 无数据
 * 效应）维持静默忽略（年轻路径由 TestFnd19GcC01RedoResultCode.testRedoIgnoresSaga
 * NotFound 钉住：121s 龄记录照常清理）。
 * 场景构造：手动以协调者身份发 FuncSaga/FuncSagaEnd（不走 perform）+ 孤儿决策记录
 * 注入 + redoTimer 反射驱动（TestFnd19GcC01RedoResultCode 桩形态）。
 */
public class TestFnd19GcD04SagaTtlNotFound {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "fnd19d4SagaTtl";

	// 手动rpc伪造的tid（避开OnzServer.nextOnzTid的分配空间；与其他测试类的0x...02xx段错开）
	private static final long TtlTid = 0x5CA1BEEF00000401L;
	private static final long AgedTid = 0x5CA1BEEF00000402L;
	private static final long AgedCommitTid = 0x5CA1BEEF00000403L;

	// 补偿失败开关与计数（cancel stub在参与方线程执行；@BeforeEach重置，volatile供跨线程读取）
	static volatile boolean CancelFail;
	static volatile int CancelCount;

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		CancelFail = false;
		CancelCount = 0;
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		var dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome));

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestFnd19GcD04SagaTtlNotFound::sagaBusiness, TestFnd19GcD04SagaTtlNotFound::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);

		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		// before() 被 Assumption 跳过时 onzServer 尚未创建；stop幂等
		if (onzServer != null) {
			// 恢复TTL默认：App.Instance跨测试类持久，残留的小超时会被60s周期清理定时器
			// 用于后续测试类的上下文（TestOnzSagaCleanup族同因未恢复而心存此患，这里止损）。
			App.Instance.Zeze.getOnz().setSagaContextTimeoutMs(Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs);
			onzServer.stop();
		}
		zeze2.Stop();
	}

	private static long sagaBusiness(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney()); // 参与方本地提交（发结果即提交）
		result.setMoney(account.getMoney());
		return 0;
	}

	/** 补偿失败注入：返回非0（业务rc原样线上携带，非moduleId组合值）——参与方按契约放回上下文等重发。 */
	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		CancelCount++;
		return CancelFail ? 100 : 0;
	}

	/**
	 * B1核心红测：构造年龄超TTL但补偿失败放回（刷新过lastActiveTime）的上下文不得被清。
	 * 修复前按startTime计时：构造时刻超龄即被清，重试链被TTL掐断。对照：无活动超龄仍被兜底清理。
	 */
	@Test
	@Timeout(120)
	public void testCleanupTimesByLastActiveTime() throws Exception {
		waitOnzReady();
		var onz = App.Instance.Zeze.getOnz();

		// 构造上下文（业务立即提交成功，滞留等FuncSagaEnd）
		sendFuncSaga(TtlTid);
		waitUntil(() -> sagaCount(onz) == 1, 30_000, "saga上下文未注册");

		// 拉开构造年龄：≥1s > 判定预算500ms
		Thread.sleep(1_000);

		// FuncSagaEnd(cancel)补偿失败：上下文放回sagas并刷新lastActiveTime
		CancelFail = true;
		awaitFutureQuietly(sendFuncSagaEnd(TtlTid, true));
		Assertions.assertEquals(1, CancelCount, "补偿必须恰好执行一次且失败");
		Assertions.assertEquals(1, sagaCount(onz), "补偿失败后上下文必须放回sagas等重发");

		// 构造年龄(≥1s)超预算、最后活动年龄（刚刷新，毫秒级）未超：不得清理。
		onz.setSagaContextTimeoutMs(500);
		onz.cleanupTimeoutSagas();
		Assertions.assertEquals(1, sagaCount(onz),
				"刷新过lastActiveTime的上下文不得被清（修复前按构造时刻计时即被清，补偿重试链被TTL掐断）");

		// 对照：无活动超龄 → 兜底回收仍生效
		onz.setSagaContextTimeoutMs(1);
		Thread.sleep(50);
		onz.cleanupTimeoutSagas();
		Assertions.assertEquals(0, sagaCount(onz), "无活动超龄的上下文必须被兜底清理");
	}

	/**
	 * C核心红测：rollback决策的超龄NotFound必须error告警（按tid去重）并保留决策记录。
	 * 修复前：静默忽略并删除记录——补偿已因参与方TTL清理而丢失，协调者侧无任何信号。
	 */
	@Test
	@Timeout(120)
	public void testRedoAgedRollbackNotFoundKeepsRecordAndWarnsOnce() throws Exception {
		waitOnzReady();
		// 无参与方上下文（不sendFuncSaga）：redo补发FuncSagaEnd(cancel)必得eSagaNotFound。
		writeOrphanRecords(AgedTid, AbstractOnz.ePreparing, Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs + 100_000);

		invokeRedoTimer();

		Assertions.assertEquals(1, count(tableOf("commitIndex")),
				"超龄NotFound必须保留决策记录（人工对账需要记录在场；修复前：静默忽略并删除，补偿丢失无信号）");
		Assertions.assertEquals(1, count(tableOf("commitPoint")), "两表同生命周期（FND4-88）：一起保留");
		Assertions.assertTrue(agedNotFoundWarnedTids().contains(AgedTid),
				"超龄rollback决策的NotFound必须触发error告警（修复前：静默忽略）");

		// 第二轮redo：保留的记录重发→再次超龄NotFound→按tid去重不重复告警，记录维持保留
		invokeRedoTimer();
		Assertions.assertEquals(1, agedNotFoundWarnedTids().size(), "告警按tid去重：每tid只error一次（对齐hangWarnedTids）");
		Assertions.assertEquals(1, count(tableOf("commitIndex")), "保留的超龄决策记录维持，等人工对账");
		Assertions.assertEquals(0, CancelCount, "无参与方上下文不得触发补偿");
	}

	/** C对照组：commit决策（cancel=false的end无数据效应）的NotFound维持静默——即使超龄也不分诊，记录照常清理。 */
	@Test
	@Timeout(120)
	public void testRedoAgedCommitNotFoundStaysSilent() throws Exception {
		waitOnzReady();
		writeOrphanRecords(AgedCommitTid, AbstractOnz.eCommitting, Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs + 100_000);

		invokeRedoTimer();

		Assertions.assertEquals(0, count(tableOf("commitIndex")),
				"commit决策的NotFound维持静默忽略，记录照常清理");
		Assertions.assertEquals(0, count(tableOf("commitPoint")), "两表同生命周期：一起清理");
		Assertions.assertFalse(agedNotFoundWarnedTids().contains(AgedCommitTid),
				"commit决策（cancel=false）不得触发超龄NotFound分诊");
	}

	/** 手动以协调者身份向zeze1发起FuncSaga（不走perform）：参与方注册上下文并提交业务，滞留等FuncSagaEnd。 */
	private void sendFuncSaga(long tid) throws Exception {
		var arg = new BKuafu.Data();
		arg.setAccount(410);
		arg.setMoney(40);
		var bb = ByteBuffer.Allocate();
		arg.encode(bb);
		var r = new Zeze.Builtin.Onz.FuncSaga();
		r.Argument.setOnzTid(tid);
		r.Argument.setFuncName(SagaName);
		r.Argument.setFuncArgument(new Binary(bb.Bytes, 0, bb.WriteIndex));
		r.Argument.setFlushMode(AbstractOnz.eFlushImmediately);
		r.SendForWait(onzServer.getZezeInstance("zeze1")); // 不await：应答要等业务+flush，这里只需上下文就位
	}

	/** 手动发送FuncSagaEnd（补偿失败注入路径），future由调用方收尾等待。 */
	private Zeze.Util.TaskCompletionSource<?> sendFuncSagaEnd(long tid, boolean cancel) throws Exception {
		var r = new Zeze.Builtin.Onz.FuncSagaEnd();
		r.Argument.setOnzTid(tid);
		r.Argument.setCancel(cancel);
		return r.SendForWait(onzServer.getZezeInstance("zeze1"), 60_000);
	}

	/** 应答收尾等待：补偿失败应答rc=100，这里只保证参与方处理完成（放回+刷新已发生）。 */
	private static void awaitFutureQuietly(Zeze.Util.TaskCompletionSource<?> future) {
		try {
			future.get(30_000, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (Exception ignored) {
		}
	}

	/** 手写孤儿决策两表（协调者崩溃残留形态，含"saga="前缀参与方）；ageMs=索引时戳回拨量。 */
	private void writeOrphanRecords(long tid, int state, long ageMs) throws Exception {
		var key = new byte[8];
		ByteBuffer.longBeHandler.set(key, 0, tid);
		var saved = new BSavedCommits.Data();
		saved.getOnzs().add("saga=zeze1"); // OH1-F1持久化编码：前缀区分saga参与方
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf("commitPoint").put(key, java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - ageMs);
		tableOf("commitIndex").put(key, java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
	}

	private static int sagaCount(Zeze.Onz.Onz onz) throws Exception {
		var field = Zeze.Onz.Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var map = (Zeze.Util.LongConcurrentHashMap<Object>)field.get(onz);
		return map.size();
	}

	@SuppressWarnings("unchecked")
	private java.util.Set<Long> agedNotFoundWarnedTids() throws Exception {
		var f = OnzServer.class.getDeclaredField("agedNotFoundWarnedTids");
		f.setAccessible(true);
		return (java.util.Set<Long>)f.get(onzServer);
	}

	private void invokeRedoTimer() throws Exception {
		var m = OnzServer.class.getDeclaredMethod("redoTimer");
		m.setAccessible(true);
		m.invoke(onzServer);
	}

	@SuppressWarnings("unchecked")
	private RocksDatabase.Table tableOf(String fieldName) throws Exception {
		Field f = OnzServer.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		return (RocksDatabase.Table)f.get(onzServer);
	}

	private static long count(RocksDatabase.Table table) throws Exception {
		long n = 0;
		try (var it = table.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next())
				n++;
		}
		return n;
	}

	private interface Condition {
		boolean test() throws Exception;
	}

	private static void waitUntil(Condition condition, int timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.test()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message);
			//noinspection BusyWait
			Thread.sleep(50);
		}
	}

	// 同 TestOnz.waitOnzReady：等订阅发现两侧集群并建连（getZezeInstance成功即perform就绪）。
	private void waitOnzReady() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 60_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				onzServer.getZezeInstance("zeze2");
				return;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
	}

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
}
