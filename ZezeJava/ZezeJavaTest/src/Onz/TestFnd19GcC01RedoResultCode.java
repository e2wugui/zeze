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
 * FND19 GC-C01 回归：redo() 对参与方应答只 await 不查结果码即删决策记录。
 * 参与方补偿失败（stub.end返回非0）按契约把上下文放回sagas等重发
 * （Onz.ProcessFuncSagaEndRequest），redo是崩溃/失败后唯一的自动重试通道——
 * 修复前该非0应答被无视、两表记录被删，补偿永久丢失（已提交saga步骤静默
 * 部分提交分歧，同文件commit()有完整对照组）。修复后对齐commit()的形态：
 * 非0码保留记录等下一轮redo幂等收敛；eSagaNotFound（上下文已清理无补偿对象）
 * 可辨识忽略。
 */
public class TestFnd19GcC01RedoResultCode {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String SagaName = "fnd19c1SagaRedo";

	// 手动rpc伪造的孤儿决策tid（避开OnzServer.nextOnzTid的分配空间）
	private static final long CancelFailTid = 0x5CA1BEEF00000201L;
	private static final long NotFoundTid = 0x5CA1BEEF00000202L;

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

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestFnd19GcC01RedoResultCode::sagaBusiness, TestFnd19GcC01RedoResultCode::sagaCancel,
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
	 * 核心红测：参与方补偿失败（非0应答）后redo必须保留决策记录——修复前await后无条件
	 * removeCommitRecord，掐断唯一自动重试通道。故障恢复（补偿成功）后下一轮redo完成收敛。
	 */
	@Test
	@Timeout(120)
	public void testRedoKeepsRecordWhenCompensationFails() throws Exception {
		waitOnzReady();
		CancelFail = true;
		startSagaContext(CancelFailTid);
		writeOrphanRecords(CancelFailTid, AbstractOnz.ePreparing);

		invokeRedoTimer();

		Assertions.assertTrue(CancelCount >= 1, "参与方补偿必须被redo触发过");
		Assertions.assertEquals(1, count(tableOf("commitIndex")),
				"补偿失败（非0应答）必须保留决策记录等下一轮redo重试"
						+ "（修复前：await后无条件删除，已提交步骤永久未补偿）");
		Assertions.assertEquals(1, count(tableOf("commitPoint")), "两表同生命周期（FND4-88）：索引保留则点表保留");
		Assertions.assertEquals(1, sagaCount(App.Instance.Zeze.getOnz()),
				"参与方补偿失败后上下文必须仍在（按契约放回sagas等重发）");

		// 故障恢复：补偿成功后保留的记录驱动重试，redo完成收敛。
		CancelFail = false;
		invokeRedoTimer();

		Assertions.assertTrue(CancelCount >= 2, "保留的记录必须驱动补偿重试");
		Assertions.assertEquals(0, count(tableOf("commitIndex")), "补偿成功后redo必须清理决策记录");
		Assertions.assertEquals(0, count(tableOf("commitPoint")), "两表同生命周期：一起清理");
		Assertions.assertEquals(0, sagaCount(App.Instance.Zeze.getOnz()), "补偿成功后参与方上下文清理");
	}

	/**
	 * eSagaNotFound契约守护（防修复矫枉过正）：参与方上下文不存在（业务失败自清理/TTL回收/
	 * 重复补发已处理）时应答eSagaNotFound——无补偿对象，redo必须忽略该码照常清理记录，
	 * 不得把NotFound当失败永久滞留。线上为moduleId组合值，解码后比较。
	 */
	@Test
	@Timeout(120)
	public void testRedoIgnoresSagaNotFound() throws Exception {
		waitOnzReady();
		writeOrphanRecords(NotFoundTid, AbstractOnz.ePreparing); // 不startSagaContext：参与方无上下文

		invokeRedoTimer();

		Assertions.assertEquals(0, CancelCount, "无上下文不得触发补偿");
		Assertions.assertEquals(0, count(tableOf("commitIndex")), "eSagaNotFound可辨识忽略，记录照常清理");
		Assertions.assertEquals(0, count(tableOf("commitPoint")), "两表同生命周期：一起清理");
	}

	/** 手动以协调者身份向zeze1发起FuncSaga（不走perform）：参与方注册上下文并提交业务，滞留等FuncSagaEnd。 */
	private void startSagaContext(long tid) throws Exception {
		var arg = new BKuafu.Data();
		arg.setAccount(310);
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

	/** 手写孤儿决策两表（协调者崩溃残留形态，含"saga="前缀参与方）。 */
	private void writeOrphanRecords(long tid, int state) throws Exception {
		var key = new byte[8];
		ByteBuffer.longBeHandler.set(key, 0, tid);
		var saved = new BSavedCommits.Data();
		saved.getOnzs().add("saga=zeze1"); // OH1-F1持久化编码：前缀区分saga参与方
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf("commitPoint").put(key, java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - 121_000); // 超龄：ePreparing需过RedoPreparingMinAgeMs
		tableOf("commitIndex").put(key, java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
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
