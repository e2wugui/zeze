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
 * FND19 GC-C02 回归：redo 的 requireNonNull+decode 在方法 try 之外，单条坏记录
 * （索引有条目而点表无，或点表值损坏截断）每轮中止整个 commitIndex 遍历——排序在其后的
 * 所有未决决策 redo 永久停滞（redoTimer循环体无按记录容错，异常冲出到DaemonTimer，
 * 下一轮从头再撞同一条毒记录）。修复：解码挪进 redo 已有的 try，毒记录按单条跳过
 * （error记tid留库人工排查），不阻塞其余记录收敛（对齐ApplyHelper的逐记录隔离形态）。
 */
public class TestFnd19GcC02RedoPoisonIsolation {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "fnd19c2SagaPoison";

	// 迭代序（key字节序）：毒记录1 < 有效记录 < 毒记录2——毒记录之前与之后的有效决策都必须被处理。
	private static final long PoisonMissingTid = 0x5CA1BEEF00000311L; // 索引有条目、点表无 → requireNonNull NPE
	private static final long ValidTid = 0x5CA1BEEF00000322L; // 有效eCommitting孤儿 → redo必须完成end
	private static final long PoisonCorruptTid = 0x5CA1BEEF00000333L; // 点表值截断 → decode异常

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		var dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome));

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestFnd19GcC02RedoPoisonIsolation::sagaBusiness, TestFnd19GcC02RedoPoisonIsolation::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);

		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		// before() 被 Assumption 跳过时 onzServer 尚未创建；stop幂等
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	private static long sagaBusiness(Zeze.Onz.OnzSaga saga, BKuafu argument, BKuafuResult result) {
		var app = (App)saga.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		return 0;
	}

	/**
	 * 核心红测：毒记录（索引无点/点值损坏）必须被单条隔离跳过，不得中止迭代——
	 * 排序在其后的有效eCommitting决策照常redo收敛。修复前：毒记录1的NPE冲出redoTimer
	 * （裸解码在try外），有效记录永轮不到，上下文与三表记录全部滞留。
	 */
	@Test
	@Timeout(120)
	public void testPoisonRecordIsolatedNotBlockingIteration() throws Exception {
		waitOnzReady();
		startSagaContext(ValidTid); // 有效记录的参与方上下文就位，等FuncSagaEnd结束

		writeIndexOnly(PoisonMissingTid, AbstractOnz.eCommitting);
		writeOrphanRecords(ValidTid, AbstractOnz.eCommitting);
		writeCorruptPoint(PoisonCorruptTid, AbstractOnz.eCommitting);

		invokeRedoTimer();

		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()),
				"毒记录不得阻塞其后有效记录的redo（修复前：迭代在毒记录上中止，上下文永久滞留）");
		Assertions.assertEquals(2, count(tableOf("commitIndex")),
				"毒记录留库人工排查、有效记录被redo清理（修复前：3条全滞留）");
		Assertions.assertEquals(1, count(tableOf("commitPoint")),
				"点表：有效记录随索引清理，只剩毒记录2的损坏值（毒记录1本就无点条目）");
	}

	/** 索引单表注入（升级遗留/人工修库形态）：有条目而点表无 → redo的requireNonNull NPE。 */
	private void writeIndexOnly(long tid, int state) throws Exception {
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - 121_000);
		tableOf("commitIndex").put(keyOf(tid), java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
	}

	/** 点表值截断（bit rot/半写形态）：decode读到一半必然越界抛运行时异常。 */
	private void writeCorruptPoint(long tid, int state) throws Exception {
		var saved = new BSavedCommits.Data();
		saved.getOnzs().add("saga=zeze1____________pad"); // 长串：4字节截断后解码必越界
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf("commitPoint").put(keyOf(tid), java.util.Arrays.copyOf(bbState.Bytes, 4));
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - 121_000);
		tableOf("commitIndex").put(keyOf(tid), java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
	}

	/** 手写孤儿决策两表（协调者崩溃残留形态，含"saga="前缀参与方）。 */
	private void writeOrphanRecords(long tid, int state) throws Exception {
		var saved = new BSavedCommits.Data();
		saved.getOnzs().add("saga=zeze1");
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf("commitPoint").put(keyOf(tid), java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - 121_000);
		tableOf("commitIndex").put(keyOf(tid), java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
	}

	private static byte[] keyOf(long tid) {
		var key = new byte[8];
		ByteBuffer.longBeHandler.set(key, 0, tid);
		return key;
	}

	/** 手动以协调者身份向zeze1发起FuncSaga（不走perform）：参与方注册上下文并提交业务，滞留等FuncSagaEnd。 */
	private void startSagaContext(long tid) throws Exception {
		var arg = new BKuafu.Data();
		arg.setAccount(320);
		arg.setMoney(30);
		var bb = ByteBuffer.Allocate();
		arg.encode(bb);
		var r = new Zeze.Builtin.Onz.FuncSaga();
		r.Argument.setOnzTid(tid);
		r.Argument.setFuncName(SagaName);
		r.Argument.setFuncArgument(new Binary(bb.Bytes, 0, bb.WriteIndex));
		r.Argument.setFlushMode(AbstractOnz.eFlushImmediately);
		r.SendForWait(onzServer.getZezeInstance("zeze1")); // 不await：应答要等业务+flush，这里只需上下文就位
		waitUntil(() -> sagaCount(App.Instance.Zeze.getOnz()) == 1, 30_000, "saga上下文未注册");
	}

	private static int sagaCount(Zeze.Onz.Onz onz) throws Exception {
		var field = Zeze.Onz.Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var map = (Zeze.Util.LongConcurrentHashMap<Object>)field.get(onz);
		return map.size();
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

	private static void waitUntil(Condition condition, long timeoutMs, String message) throws Exception {
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
