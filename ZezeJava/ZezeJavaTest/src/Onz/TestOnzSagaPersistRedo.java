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
 * OH1-F1/OH1-F4 回归：
 * <b>OH1-F1（P1）</b>：buildSavedCommits原先只收集zezeProcedures——saga事务该集合恒空，
 * 协调者崩溃（或cancelSaga的FuncSagaEnd丢失且不重试）后，redoTimer对残留决策记录解出
 * 空参与方列表直接removeCommitRecord：已提交步骤永久未补偿——静默部分提交分歧。
 * 修复：saga参与方以"saga="前缀持久化进BSavedCommits.Onzs（集群名不含'='，零碰撞；
 * bean为生成代码不可加字段），redo按参与方类型分流——saga参与方发FuncSagaEnd
 * （eCommitting=end，ePreparing=cancel补偿）。
 * <b>OH1-F4（P3）</b>：perform的buildSavedCommits快照原先定格在waitPendingAsync之前，
 * pendingAsync窗口内注册的参与方不进eCommitting持久化，redo补发缺迟到者。修复：
 * 窗口后重建快照再传给commit（592行ePreparing快照不动，保FND5-44/FND6-36契约）。
 */
public class TestOnzSagaPersistRedo {
	// 过程名必须全JVM唯一：demo.App单例的Onz注册表跨测试类持久。
	private static final AtomicBoolean registeredOnAppInstance = new AtomicBoolean();
	private static final String ProcName = "cp1f4CommitSnap";
	private static final String SagaName = "oh1f1SagaRedo";

	// 手动rpc伪造的孤儿决策tid（避开OnzServer.nextOnzTid的分配空间）
	private static final long OrphanCancelTid = 0x5CA1BEEF00000101L;
	private static final long OrphanEndTid = 0x5CA1BEEF00000102L;

	// saga补偿/end记账（cancel stub在参与方线程执行；@BeforeEach重置，volatile供跨线程轮询）
	static volatile int CancelCount;

	private final App zeze2 = new App();
	private OnzServer onzServer;
	private String dbHome;

	@BeforeEach
	public void before() throws Exception {
		CancelCount = 0;
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome));

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		// zeze1: saga参与方（快业务+记账cancel/end）；zeze2: procedure参与方（OH1-F4迟到注册用）
		if (registeredOnAppInstance.compareAndSet(false, true)) {
			App.Instance.Zeze.getOnz().registerSaga(SagaName,
					TestOnzSagaPersistRedo::sagaBusiness, TestOnzSagaPersistRedo::sagaCancel,
					BKuafu.class, BKuafuResult.class, Zeze.Transaction.EmptyBean.class);
			App.Instance.Zeze.getOnz().register(ProcName,
					TestOnzSagaPersistRedo::procBusiness, BKuafu.class, BKuafuResult.class);
		}
		zeze2.Zeze.getOnz().register(ProcName,
				TestOnzSagaPersistRedo::procBusiness, BKuafu.class, BKuafuResult.class);

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
		account.setMoney(account.getMoney() + argument.getMoney()); // 参与方本地提交（sendReadyAndWait=发结果即提交）
		result.setMoney(account.getMoney());
		return 0;
	}

	private static long sagaCancel(Zeze.Onz.OnzSaga saga, Zeze.Transaction.EmptyBean cancelArgument) {
		CancelCount++;
		return 0;
	}

	private static long procBusiness(Zeze.Onz.OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		var app = (App)onzProcedure.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	/**
	 * OH1-F4：pendingAsync窗口内注册的参与方必须进入传给txn.commit的快照
	 * （eCommitting持久化完整参与方列表）。
	 * 修复前：commit收到的是窗口前的陈旧快照（只有zeze2），迟到参与方zeze1的
	 * Commit只靠活map发送、崩溃后redo补发缺它——ready超时自愈回滚 vs 协调者已报成功。
	 */
	@Test
	@Timeout(120)
	public void testCommitSnapshotIncludesPendingAsyncParticipants() throws Exception {
		waitOnzReady();
		var txn = new Zeze.Onz.LateRegisterTransaction();
		txn.setOnzServer(onzServer);
		Assertions.assertEquals(0L, onzServer.perform(txn), "perform必须成功");

		var onzs = txn.capturedCommitState.getOnzs();
		Assertions.assertTrue(onzs.contains("zeze2"), "同步注册的参与方必须在快照内");
		Assertions.assertTrue(onzs.contains("zeze1"),
				"pendingAsync窗口内注册的参与方必须进入commit快照（修复前快照定格在窗口之前）");
	}

	/**
	 * OH1-F1（ePreparing孤儿→补偿）：参与方业务已提交、上下文滞留等FuncSagaEnd；
	 * 协调者崩溃留下含"saga=zeze1"的ePreparing残留——redo必须向该参与方发
	 * FuncSagaEnd(cancel=true)完成补偿并清理两表。
	 * 修复前：Onzs只可能是procedure集群名（或旧ip_port），"saga=zeze1"被当集群名/ip_port
	 * 处理，连接失败→记录滞留或（空列表时）直接删除，补偿永久丢失。
	 */
	@Test
	@Timeout(120)
	public void testOrphanPreparingSagaRedoneWithCancel() throws Exception {
		waitOnzReady();
		startSagaContext(OrphanCancelTid);

		writeOrphanRecords(OrphanCancelTid, AbstractOnz.ePreparing);

		invokeRedoTimer();

		// 核心（红断言）：saga参与方必须收到补偿
		waitUntil(() -> CancelCount >= 1, 30_000,
				"ePreparing残留的redo必须向saga参与方补发FuncSagaEnd(cancel=true)（修复前补偿永久丢失）");
		Assertions.assertEquals(0, count(tableOf("commitIndex")), "redo完成后索引清理");
		Assertions.assertEquals(0, count(tableOf("commitPoint")), "redo完成后点表清理");
	}

	/**
	 * OH1-F1（eCommitting孤儿→结束）：协调者崩溃在commit决策落盘后、endSaga完成前——
	 * redo必须发FuncSagaEnd(cancel=false)结束参与方上下文（幂等，重复发送无害）。
	 * 修复前：残留记录被直接删除，参与方上下文滞留sagas（1小时超时清理前占用rpc与bean）。
	 */
	@Test
	@Timeout(120)
	public void testOrphanCommittingSagaRedoneWithEnd() throws Exception {
		waitOnzReady();
		startSagaContext(OrphanEndTid);

		writeOrphanRecords(OrphanEndTid, AbstractOnz.eCommitting);

		invokeRedoTimer();

		waitUntil(() -> sagaCount(App.Instance.Zeze.getOnz()) == 0, 30_000, "saga上下文必须被FuncSagaEnd结束清理");
		Assertions.assertEquals(0, CancelCount, "eCommitting补发的是结束不是补偿");
		Assertions.assertEquals(0, count(tableOf("commitIndex")), "redo完成后索引清理");
	}

	/** 手动以协调者身份向zeze1发起FuncSaga（不走perform）：参与方注册上下文并提交业务，滞留等FuncSagaEnd。 */
	private void startSagaContext(long tid) throws Exception {
		var arg = new BKuafu.Data();
		arg.setAccount(300);
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
