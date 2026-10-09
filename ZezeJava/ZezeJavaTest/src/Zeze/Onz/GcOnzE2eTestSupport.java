package Zeze.Onz;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import demo.App;
import harness.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * FND19 Gc系Onz e2e测试共享脚手架（TestFnd19GcC01-C04/GcD04）：进程内两集群
 * （zeze1=demo.App本进程、zeze2=zeze_cluster_2.xml）与OnzServer协调者的启停样板，
 * 以及跨测试同构的反射/等待/孤儿决策注入助手（对齐Dbh2侧Fnd19GADStubSupport共享先例）。
 * 各测试保留自己的场景与断言；场景变体（带时戳参数的记录注入、慢业务/免等待的FuncSaga
 * 发送等）留在各自文件。
 */
final class GcOnzE2eTestSupport {
	private GcOnzE2eTestSupport() {
	}

	/** before()样板（前半）：Assumption探活→清理OnzServer库目录→启动两集群；per-test的清表与参与方注册在两次样板调用之间。 */
	static Config startTwoClusters(App zeze2) throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		deleteRecursively(Path.of("CommitOnzServer" + myConfig.getServerId()));

		App.Instance.Start();
		zeze2.Start(Config.load("./zeze_cluster_2.xml"));
		return myConfig;
	}

	/** before()样板（后半）：构造并启动协调者OnzServer（zeze1=本进程，zeze2=第二集群）。 */
	static OnzServer startOnzServer(Config myConfig) throws Exception {
		var onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
		return onzServer;
	}

	/** after()样板：before()被Assumption跳过时onzServer尚未创建；stop幂等。 */
	static void stopCoordinator(OnzServer onzServer, App zeze2) throws Exception {
		if (onzServer != null) {
			awaitOnzFlushSettled(onzServer);
			onzServer.stop();
		}
		zeze2.Stop();
	}

	/**
	 * 参与方 finalCommit-flush 排空后再拆协调者。perform 的业务应答先于参与方事务的
	 * finalCommit 发出（2026-10-09批r1实证：应答.769、两参与方flush .783——ZezeTaskPool
	 * 派发排队~20ms），after() 立即 stop 掐断在途 FlushReady：resolveFlushSocket null/
	 * 非0应答→finalCommit异常→halt(543543) 整IT相陪葬。产品契约"出错即halt比重试稳健"
	 * （2026-10-09用户裁定），排空责任在测试侧。观察点=OnzAgent 连接的收包计数静默
	 * 300ms：FlushReady 请求是业务应答之后的最后一批入包（协调者侧事务已结束，走
	 * OnzAgent.ProcessFlushReadyRequest 的幂等放行应答0）。计数含closed累计（单调），
	 * 误静默窗口=派发队列静默超过300ms的极端满载，远窄于原先的必然竞态窗口。
	 * 有界5s，超时不再等——真滞留由产品halt如实暴露，不被掩盖。
	 */
	static void awaitOnzFlushSettled(OnzServer onzServer) throws Exception {
		var agentField = OnzServer.class.getDeclaredField("onzAgent");
		agentField.setAccessible(true);
		var service = ((OnzAgent)agentField.get(onzServer)).getService();
		long lastRecv = -1;
		long quietSince = System.currentTimeMillis();
		var deadline = quietSince + 5_000;
		while (System.currentTimeMillis() < deadline) {
			service.updateRecvSendSize();
			var recv = service.getRecvCount();
			if (recv != lastRecv) {
				lastRecv = recv;
				quietSince = System.currentTimeMillis();
			} else if (System.currentTimeMillis() - quietSince >= 300)
				return;
			Thread.sleep(50);
		}
	}

	// 同 TestOnz.waitOnzReady：等订阅发现两侧集群并建连（getZezeInstance成功即perform就绪）。
	static void waitOnzReady(OnzServer onzServer) throws InterruptedException {
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

	static int sagaCount(Zeze.Onz.Onz onz) throws Exception {
		var field = Zeze.Onz.Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var map = (Zeze.Util.LongConcurrentHashMap<Object>)field.get(onz);
		return map.size();
	}

	static void invokeRedoTimer(OnzServer onzServer) throws Exception {
		var m = OnzServer.class.getDeclaredMethod("redoTimer");
		m.setAccessible(true);
		m.invoke(onzServer);
	}

	static RocksDatabase.Table tableOf(OnzServer onzServer, String fieldName) throws Exception {
		Field f = OnzServer.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		return (RocksDatabase.Table)f.get(onzServer);
	}

	static long count(RocksDatabase.Table table) throws Exception {
		long n = 0;
		try (var it = table.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next())
				n++;
		}
		return n;
	}

	/** 手写孤儿决策两表（协调者崩溃残留形态，含"saga="前缀参与方）；时戳回拨121s：ePreparing需超RedoPreparingMinAgeMs才被redo。 */
	static void writeOrphanRecords(OnzServer onzServer, long tid, int state) throws Exception {
		var key = new byte[8];
		ByteBuffer.longBeHandler.set(key, 0, tid);
		var saved = new BSavedCommits.Data();
		saved.getOnzs().add("saga=zeze1"); // OH1-F1持久化编码：前缀区分saga参与方
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf(onzServer, "commitPoint").put(key, java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - 121_000);
		tableOf(onzServer, "commitIndex").put(key, java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
	}

	/** 手动以协调者身份向zeze1发起FuncSaga（不走perform）：参与方注册上下文并提交业务，滞留等FuncSagaEnd；money=30与各用例既有值一致。 */
	static void startSagaContext(OnzServer onzServer, String sagaName, long account, long tid) throws Exception {
		var arg = new demo.Module1.BKuafu.Data();
		arg.setAccount(account);
		arg.setMoney(30);
		var bb = ByteBuffer.Allocate();
		arg.encode(bb);
		var r = new Zeze.Builtin.Onz.FuncSaga();
		r.Argument.setOnzTid(tid);
		r.Argument.setFuncName(sagaName);
		r.Argument.setFuncArgument(new Binary(bb.Bytes, 0, bb.WriteIndex));
		r.Argument.setFlushMode(AbstractOnz.eFlushImmediately);
		r.SendForWait(onzServer.getZezeInstance("zeze1")); // 不await：应答要等业务+flush，这里只需上下文就位
		waitUntil(() -> sagaCount(App.Instance.Zeze.getOnz()) == 1, 30_000, "saga上下文未注册");
	}

	interface Condition {
		boolean test() throws Exception;
	}

	static void waitUntil(Condition condition, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.test()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message);
			//noinspection BusyWait
			Thread.sleep(50);
		}
	}

	static void deleteRecursively(Path root) throws Exception {
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
