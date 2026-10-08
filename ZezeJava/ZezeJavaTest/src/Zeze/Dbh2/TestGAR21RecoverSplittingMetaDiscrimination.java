package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.LogSetSplittingMeta;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Acceptor;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 R2-1回归：recoverSplitting的move/split身份判别必须是纯元数据判别
 * （splitting与源桶meta同keyFirst且同keyLast即move；split的splitting.keyFirst=
 * locateMiddle中位key恒严格大于源keyFirst）。判别只读meta，不受拷贝窗口内数据增删影响。
 * 旧"data[0]==keyFirst"启发式依赖数据形态：源桶事务delete是物理删除（无墓碑），
 * [F,M)左半段被时间序/TTL负载清空后data[0]右移到分界key M，真split被误判为move——
 * move收尾endMove从apply时刻data[0]删到尾+源桶置死桶meta{1},{1}，[F,M)窗口写入静默
 * 物理删除、键域永久失联且无自愈（master侧settle守卫对from==null的LogEndMove路径
 * 结构性不触发）。双向各一用例：
 * ① split误判方向（本案）：左半段全删+data[0]==M场景，断言走split收尾——源桶收窄为
 * [F,M)而非死桶、[F,M)键域分桶完成后可继续写入（bug时死桶写入eBucketNotFound）；
 * ② move误判方向回归（GA-C01反向）：边界key F被删后data[0]&gt;F，旧启发式判split，
 * 断言走move收尾——源桶死桶{1},{1}（move正确终态），不得留下split收窄的空区间桶。
 * serverId 833段为本用例族预留（本测试不经Dbh2AgentManager，raw agent直连，无占用）。
 * 形态：进程内双3节点raft桶（源+目标，19160-19165段）+leader上直构splitting持久态
 * （appendLog LogSetSplittingMeta）+反射触发私有recoverSplitting（onLeaderReady挂点）；
 * 桩master对EndSplit/EndMove恒回0（终结通知可观测且不产生重试泄漏）。
 */
@Fast
public class TestGAR21RecoverSplittingMetaDiscrimination {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 端口段与既有Dbh2测试错开：TestSplitPutTombstone=19100/19110、GA01=19130-32、
	// GA02=19140-42、GAD02=19150-52；本测试源桶=19160-62、目标桶=19163-65。
	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19160"/>
				<node Host="127.0.0.1" Port="19161"/>
				<node Host="127.0.0.1" Port="19162"/>
			</raft>
			""";

	private static final String TARGET_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19163"/>
				<node Host="127.0.0.1" Port="19164"/>
				<node Host="127.0.0.1" Port="19165"/>
			</raft>
			""";

	private static final AtomicInteger tid = new AtomicInteger();

	// 桩master：对EndSplit/EndMove恒回0（桶侧收尾通知可观测；rc=0时新旧MasterAgent都不重试，
	// 不留定时器泄漏——本测试验证桶侧终态，master侧结算由TestFnd20GAC01守卫用例覆盖）。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger endRequests = new AtomicInteger();

		StubMasterService(int port) {
			super("stubMasterFnd20GAR21", stubServerConfig(port));
			setNoProcedure(true);

			var fhSplit = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndSplit.class, EndSplit.TypeId_);
			fhSplit.Factory = EndSplit::new;
			fhSplit.Handle = r -> onEnd(r);
			fhSplit.Level = TransactionLevel.None;
			fhSplit.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndSplit.TypeId_, fhSplit);

			var fhMove = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndMove.class, EndMove.TypeId_);
			fhMove.Factory = EndMove::new;
			fhMove.Handle = r -> onEnd(r);
			fhMove.Level = TransactionLevel.None;
			fhMove.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndMove.TypeId_, fhMove);
		}

		private long onEnd(EndSplit r) {
			endRequests.incrementAndGet();
			r.SendResultCode(0);
			return 0;
		}

		private long onEnd(EndMove r) {
			endRequests.incrementAndGet();
			r.SendResultCode(0);
			return 0;
		}
	}

	private static Config stubServerConfig(int port) {
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put("stubMasterFnd20GAR21", conf);
		return config;
	}

	private static MasterAgent stubMasterAgent(int port) {
		var conf = new ServiceConf();
		conf.addConnector(new Connector("127.0.0.1", port, true));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, conf);
		return new MasterAgent(config);
	}

	// Dbh2.startSplit/endSplit2需要manager（atomicSerialNo与getMasterAgent）；
	// 复用Dbh2Manager实例但把getMasterAgent指向桩连接（不start，无端口/定时器副作用）。
	private static final class Dbh2ManagerStub extends Zeze.Dbh2.Dbh2Manager {
		final Path home;
		private final MasterAgent stubAgent;

		Dbh2ManagerStub(Path home, String configXml, MasterAgent stubAgent) throws Exception {
			super(home.toString(), configXml);
			this.home = home;
			this.stubAgent = stubAgent;
		}

		@Override
		public MasterAgent getMasterAgent() {
			return stubAgent;
		}
	}

	private static Dbh2ManagerStub newStubbedManager(MasterAgent stubAgent) throws Exception {
		// home在系统临时目录而非@TempDir：manager内部rocks句柄只能反射关闭，
		// 残留不应让JUnit的@TempDir收尾删除失败（Dbh2TestEnv同款取舍）。
		var home = Files.createTempDirectory("fnd20gar21-manager");
		var xml = home.resolve("manager.xml");
		Files.writeString(xml, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<zeze/>\n");
		return new Dbh2ManagerStub(home, xml.toString(), stubAgent);
	}

	// manager从未start，其内部rocks句柄反射关闭后尽力删除home（失败tolerated：Windows句柄延迟）。
	private static void closeManagerQuietly(Dbh2ManagerStub manager) {
		try {
			var field = Zeze.Dbh2.Dbh2Manager.class.getDeclaredField("database");
			field.setAccessible(true);
			((RocksDatabase)field.get(manager)).close();
		} catch (Exception e) {
			// best effort
		}
		LogSequence.deleteDirectory(manager.home.toFile());
	}

	private static ArrayList<Zeze.Dbh2.Dbh2> startBucket(Zeze.Dbh2.Dbh2Manager manager, RocksDatabase database,
														 String raftConfigString, Path tempDir, String homePrefix) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			// 每节点独立loadFromString（Raft构造改写配置对象，共享致节点身份错乱）。
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(homePrefix + config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(manager, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static Binary value(int i) {
		return new Binary(new byte[]{(byte)(0x10 + i)});
	}

	private static BBucketMeta.Data metaOf(Binary keyFirst, Binary keyLast, String raftConfig) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table1");
		meta.setRaftConfig(raftConfig);
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		return meta;
	}

	private static void setBucketMeta(Dbh2Agent agent, Binary keyFirst, Binary keyLast) {
		agent.setBucketMeta(metaOf(keyFirst, keyLast, ""));
	}

	private static void put(Dbh2Agent agent, int k) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().put(key(k), value(k));
		batch.getBatch().setTid(tid.incrementAndGet());
		var f = agent.prepareBatch(batch);
		f.await();
		Assertions.assertEquals(0, f.get().getResultCode(), "预填写入必须成功");
		Assertions.assertEquals(0, agent.commitBatch(batch.getBatch().getTid()).await().get().getResultCode(), "预填提交必须成功");
	}

	private static void delete(Dbh2Agent agent, int k) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getDeletes().add(key(k));
		batch.getBatch().setTid(tid.incrementAndGet());
		var f = agent.prepareBatch(batch);
		f.await();
		Assertions.assertEquals(0, f.get().getResultCode(), "删除写入必须成功");
		Assertions.assertEquals(0, agent.commitBatch(batch.getBatch().getTid()).await().get().getResultCode(), "删除提交必须成功");
	}

	private static Binary getValue(Dbh2Agent agent, int k) {
		var kv = agent.get("database", "table1", key(k));
		Assertions.assertTrue(kv.getKey(), "get必须命中属主桶");
		return kv.getValue() == null ? null
				: new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size());
	}

	// 恢复入口=onLeaderReady挂点（Dbh2构造注册recoverSplitting），私有方法反射触发。
	private static void invokeRecoverSplitting(Zeze.Dbh2.Dbh2 leader) throws Exception {
		Method method = Zeze.Dbh2.Dbh2.class.getDeclaredMethod("recoverSplitting");
		method.setAccessible(true);
		method.invoke(leader);
	}

	private static void waitSplittingApplied(Zeze.Dbh2.Dbh2 leader) throws Exception {
		var deadline = System.currentTimeMillis() + 10_000;
		while (leader.getStateMachine().getBucket().getSplittingMeta() == null) {
			if (System.currentTimeMillis() > deadline)
				throw new IllegalStateException("LogSetSplittingMeta未apply（注入失败）");
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	// 终态=源桶splitting消费完毕+收尾通知（endSplit2的EndSplit/EndMove）到达桩master。
	private static void waitFinished(Zeze.Dbh2.Dbh2 leader, StubMasterService server) throws Exception {
		var deadline = System.currentTimeMillis() + 30_000;
		while (leader.getStateMachine().getBucket().getSplittingMeta() != null || server.endRequests.get() == 0) {
			if (System.currentTimeMillis() > deadline)
				throw new IllegalStateException("recoverSplitting收尾链未完成（卡死）");
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	/**
	 * ① split误判方向（R2-1本案）：[F,M)左半段全部物理删除、data[0]恰为分界key M的场景，
	 * 恢复必须判split。源桶数据[2..7]（6条）真实split的中位（locateMiddle走keyNumbers/2=3步）
	 * 恰为5，splitting=[5,8)与真实startSplit构造一致；随后删2,3,4制造data[0]==M。
	 * bug（旧启发式data[0]==keyFirst）判move：endMove把源桶置死桶{1},{1}+从data[0]删到尾，
	 * [F,M)键域永久失效（写入eBucketNotFound）。修复判split：源桶收窄[F,M)持续可写，
	 * [M,L)复制到新桶。
	 */
	@Test
	public void testSplitLeftHalfDeletedKeepsSourceNarrowedWritable(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var sourceLogDb = new RocksDatabase(tempDir.resolve("gar21-src-log").toString());
		var targetLogDb = new RocksDatabase(tempDir.resolve("gar21-dst-log").toString());
		var manager = newStubbedManager(masterAgent);
		var source = startBucket(manager, sourceLogDb, SOURCE_RAFT, tempDir, "src");
		var target = startBucket(manager, targetLogDb, TARGET_RAFT, tempDir, "dst");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		var targetAgent = new Dbh2Agent(TARGET_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			RaftBucketTopologySupport.waitLeader(target);

			// 拷贝前形态：源桶[F,L)=[2,8)，6条数据，真实split的splitting=[M,L)=[5,8)。
			setBucketMeta(sourceAgent, key(2), key(8));
			for (int k = 2; k <= 7; ++k)
				put(sourceAgent, k);

			// 拷贝窗口内[T2]：[F,M)左半段被时间序负载全部物理删除（delete是物理删除，data[0]右移到M）。
			for (int k = 2; k <= 4; ++k)
				delete(sourceAgent, k);

			// 直构"split进行中"持久态：splitting=[5,8)（startSplit对split=copy源meta后仅改
			// keyFirst为中位key，keyLast不动），raftConfig指向真实目标桶——与崩溃前的raft落盘形态一致。
			leader.getRaft().appendLog(new LogSetSplittingMeta(metaOf(key(5), key(8), TARGET_RAFT)));
			waitSplittingApplied(leader);

			// leader重启（onLeaderReady）→recoverSplitting。
			invokeRecoverSplitting(leader);
			waitFinished(leader, server);

			// split收尾：源桶收窄为[F,M)=[2,5)（bug走move收尾置死桶{1},{1}）。
			var meta = leader.getStateMachine().getBucket().getBucketMeta();
			Assertions.assertEquals(0, key(2).compareTo(meta.getKeyFirst()),
					"split收尾源桶keyFirst必须保持F（bug：move收尾置死桶）");
			Assertions.assertEquals(0, key(5).compareTo(meta.getKeyLast()),
					"split收尾源桶必须收窄为[F,M)=[2,5)（bug：move收尾源桶keyLast=死桶标记{1}）");

			// [F,M)键域不丢：分桶完成后左半段必须可继续写入（bug：死桶inBucket恒false，
			// move历史对k<M无定位→eBucketNotFound，键域永久失效）。
			var batch = new BPrepareBatch.Data("", "database", "table1", null);
			batch.getBatch().getPuts().put(key(3), value(3));
			batch.getBatch().setTid(tid.incrementAndGet());
			var f = sourceAgent.prepareBatch(batch);
			f.await();
			Assertions.assertEquals(0, f.get().getResultCode(),
					"split收尾后[F,M)键域必须可写——bug时move收尾死桶写入eBucketNotFound（[F,M)永久失联）");
			Assertions.assertEquals(0, sourceAgent.commitBatch(batch.getBatch().getTid()).await().get().getResultCode(),
					"[F,M)键域写入必须可提交");
			Assertions.assertEquals(value(3), getValue(sourceAgent, 3), "[F,M)键域写入必须可读回");

			// [M,L)数据经复制流到达目标桶，目标桶meta=splitting=[M,L)。
			Assertions.assertEquals(value(5), getValue(targetAgent, 5), "[M,L)数据必须复制到新桶");
			Assertions.assertEquals(value(6), getValue(targetAgent, 6), "[M,L)数据必须复制到新桶");
			Assertions.assertEquals(value(7), getValue(targetAgent, 7), "[M,L)数据必须复制到新桶");
			var targetLeader = RaftBucketTopologySupport.waitLeader(target);
			Assertions.assertEquals(0, key(5).compareTo(
					targetLeader.getStateMachine().getBucket().getBucketMeta().getKeyFirst()), "新桶meta.keyFirst=M");
			Assertions.assertEquals(0, key(8).compareTo(
					targetLeader.getStateMachine().getBucket().getBucketMeta().getKeyLast()), "新桶meta.keyLast=L");

			// 源桶[M,L)已删（收窄语义），splitting标记已消费。
			Assertions.assertNull(leader.getStateMachine().getBucket().get(key(5)), "源桶[M,L)数据必须删除");
			Assertions.assertNull(leader.getStateMachine().getBucket().getSplittingMeta(), "splitting必须消费");
		} finally {
			sourceAgent.close();
			targetAgent.close();
			for (var n : source)
				n.close();
			for (var n : target)
				n.close();
			sourceLogDb.close();
			targetLogDb.close();
			masterAgent.stop();
			server.stop();
			closeManagerQuietly(manager);
		}
	}

	/**
	 * ② move误判方向回归（GA-C01反向，新判别下不再进split分支）：move的splitting与源
	 * meta同边界[F,L)=[2,8)，边界key F=2被删后data[0]=3&gt;F——旧启发式（data[0]!=keyFirst）
	 * 误判split，endSplit1构造from=[F,F)空区间、源桶meta收窄为[2,2)而非move死桶终态。
	 * 新判别（splitting.keyFirst==meta.keyFirst恒move）必须走move收尾：源桶死桶{1},{1}、
	 * 全量数据复制到目标桶、目标桶meta=[2,8)。
	 */
	@Test
	public void testMoveBoundaryKeyDeletedGoesMoveEnd(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var sourceLogDb = new RocksDatabase(tempDir.resolve("gar21-src2-log").toString());
		var targetLogDb = new RocksDatabase(tempDir.resolve("gar21-dst2-log").toString());
		var manager = newStubbedManager(masterAgent);
		var source = startBucket(manager, sourceLogDb, SOURCE_RAFT, tempDir, "src2");
		var target = startBucket(manager, targetLogDb, TARGET_RAFT, tempDir, "dst2");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		var targetAgent = new Dbh2Agent(TARGET_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			RaftBucketTopologySupport.waitLeader(target);

			// move拷贝窗口：边界key F=2被删（data[0]=3>F），其余数据存活。
			setBucketMeta(sourceAgent, key(2), key(8));
			for (int k : List.of(2, 3, 5, 7))
				put(sourceAgent, k);
			delete(sourceAgent, 2);

			// 直构"move进行中"持久态：splitting=源meta副本同边界[2,8)（startSplit对move仅清
			// raftConfig不改边界），raftConfig指向真实目标桶。
			leader.getRaft().appendLog(new LogSetSplittingMeta(metaOf(key(2), key(8), TARGET_RAFT)));
			waitSplittingApplied(leader);

			invokeRecoverSplitting(leader);
			waitFinished(leader, server);

			// move收尾：源桶死桶{1},{1}（bug误判split收尾源桶meta=[2,2)空区间桶）。
			var meta = leader.getStateMachine().getBucket().getBucketMeta();
			Assertions.assertEquals(0, new Binary(new byte[]{1}).compareTo(meta.getKeyFirst()),
					"move收尾源桶必须置死桶标记{1}（bug：误判split收尾keyFirst=F=[2,2)空区间桶）");
			Assertions.assertEquals(0, new Binary(new byte[]{1}).compareTo(meta.getKeyLast()),
					"move收尾源桶keyLast必须置死桶标记{1}");

			// 全量数据复制到目标桶，目标桶meta=[F,L)=[2,8)。
			Assertions.assertEquals(value(3), getValue(targetAgent, 3), "move必须全量复制存活数据");
			Assertions.assertEquals(value(5), getValue(targetAgent, 5), "move必须全量复制存活数据");
			Assertions.assertEquals(value(7), getValue(targetAgent, 7), "move必须全量复制存活数据");
			var targetLeader = RaftBucketTopologySupport.waitLeader(target);
			Assertions.assertEquals(0, key(2).compareTo(
					targetLeader.getStateMachine().getBucket().getBucketMeta().getKeyFirst()), "move目标桶meta.keyFirst=F");
			Assertions.assertEquals(0, key(8).compareTo(
					targetLeader.getStateMachine().getBucket().getBucketMeta().getKeyLast()), "move目标桶meta.keyLast=L");

			// 源桶数据已随move清空，splitting标记已消费。
			Assertions.assertNull(leader.getStateMachine().getBucket().get(key(3)), "源桶数据必须随move清空");
			Assertions.assertNull(leader.getStateMachine().getBucket().getSplittingMeta(), "splitting必须消费");
		} finally {
			sourceAgent.close();
			targetAgent.close();
			for (var n : source)
				n.close();
			for (var n : target)
				n.close();
			sourceLogDb.close();
			targetLogDb.close();
			masterAgent.stop();
			server.stop();
			closeManagerQuietly(manager);
		}
	}
}
