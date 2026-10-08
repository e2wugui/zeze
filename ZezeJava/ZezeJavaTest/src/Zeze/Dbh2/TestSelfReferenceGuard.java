package Zeze.Dbh2;

import harness.Extra;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Master.CheckFreeManager;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
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
 * FND21 GA-D01 A3回归：请求方自指守卫（INV3：任何桶不会以自身为目标执行move/split）。
 * (B)主害路径：move1(A→B)完成而endMove未达master（30s重试间隙），B的loadMonitor再决策
 * move，createSplitBucket同四元组幂等resume命中指向B自身的陈旧条目——B对自身
 * putIfAbsent拷贝（无错）、endMove apply从data[0]删到尾并置死桶——数据物理灭失、settle后
 * 主表指向已清空的死桶，静默无自愈。
 * 修复=startSplit在createSplitBucket返回后、appendLog(LogSetSplittingMeta)前：返回条目
 * raftConfig的sortedNames与本桶全等（新建条目恒为新端口新raft，全等只在resume到指向自身
 * 的陈旧条目时出现）→ logger.error+中止本轮（不发LogSetSplittingMeta，桶保持完整服务）。
 * 身份只在请求方本地持有（R1钉死raftConfig不能经rpc进身份判据），故拦截位在请求方。
 * 用例：
 * ①自指形态（红）：桩master的CreateSplitBucket返回raftConfig=本桶配置的条目——
 * startSplit必须中止：splittingMeta保持null（bug时LogSetSplittingMeta已apply）。
 * ②非自指形态（绿）：返回真实第二桶配置——守卫放行，分桶照常进入（LogSetSplittingMeta
 * apply可见），全流程走完（真实拷贝+settle+终局），证明不过度收紧。
 * serverId 883段与raft端口19190-19195段为本用例族预留。
 */
@Fast
@Extra
public class TestSelfReferenceGuard {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 端口段：源桶=19190-92，目标桶=19193-95（与GAD01=19180-82错开）。
	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19190"/>
				<node Host="127.0.0.1" Port="19191"/>
				<node Host="127.0.0.1" Port="19192"/>
			</raft>
			""";

	private static final String TARGET_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19193"/>
				<node Host="127.0.0.1" Port="19194"/>
				<node Host="127.0.0.1" Port="19195"/>
			</raft>
			""";

	private static final AtomicInteger tid = new AtomicInteger();

	// 桩master：CheckFreeManager恒回count=3（≥raftClusterCount，放行进入分桶段）；
	// CreateSplitBucket返回脚本指定的raftConfig条目（自指/真实目标两形态）；EndSplit回0。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger createSplitRequests = new AtomicInteger();
		volatile String replyRaftConfig = SOURCE_RAFT; // 默认自指形态

		StubMasterService(int port) {
			super("stubMasterSelfReference", stubServerConfig(port));
			setNoProcedure(true);

			var fhCheck = new Zeze.Net.Service.ProtocolFactoryHandle<>(CheckFreeManager.class, CheckFreeManager.TypeId_);
			fhCheck.Factory = CheckFreeManager::new;
			fhCheck.Handle = r -> {
				r.Result.setCount(3);
				r.SendResult();
				return 0;
			};
			fhCheck.Level = TransactionLevel.None;
			fhCheck.Mode = DispatchMode.Normal;
			AddFactoryHandle(CheckFreeManager.TypeId_, fhCheck);

			var fhCreate = new Zeze.Net.Service.ProtocolFactoryHandle<>(CreateSplitBucket.class, CreateSplitBucket.TypeId_);
			fhCreate.Factory = CreateSplitBucket::new;
			fhCreate.Handle = r -> onCreateSplit(r);
			fhCreate.Level = TransactionLevel.None;
			fhCreate.Mode = DispatchMode.Normal;
			AddFactoryHandle(CreateSplitBucket.TypeId_, fhCreate);

			var fhEnd = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndSplit.class, EndSplit.TypeId_);
			fhEnd.Factory = EndSplit::new;
			fhEnd.Handle = r -> {
				r.SendResultCode(0);
				return 0;
			};
			fhEnd.Level = TransactionLevel.None;
			fhEnd.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndSplit.TypeId_, fhEnd);
		}

		// 返回请求四元组+脚本raftConfig的完整条目（master侧createSplitBucket恒填充raftConfig）。
		private long onCreateSplit(CreateSplitBucket r) {
			createSplitRequests.incrementAndGet();
			var result = r.Argument.copy();
			result.setRaftConfig(replyRaftConfig);
			r.Result = result;
			r.SendResult();
			return 0;
		}
	}

	private static Config stubServerConfig(int port) {
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put("stubMasterSelfReference", conf);
		return config;
	}

	private static MasterAgent stubMasterAgent(int port) {
		var conf = new ServiceConf();
		conf.addConnector(new Connector("127.0.0.1", port, true));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, conf);
		return new MasterAgent(config);
	}

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
		var home = Files.createTempDirectory("self-reference-manager");
		var xml = home.resolve("manager.xml");
		Files.writeString(xml, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<zeze ServerId=\"883\"/>\n");
		return new Dbh2ManagerStub(home, xml.toString(), stubAgent);
	}

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

	private static void put(Dbh2Agent agent, int k) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().put(key(k), value(k));
		batch.getBatch().setTid(tid.incrementAndGet());
		var f = agent.prepareBatch(batch);
		f.await();
		Assertions.assertEquals(0, f.get().getResultCode(), "预填写入必须成功");
		Assertions.assertEquals(0, agent.commitBatch(batch.getBatch().getTid()).await().get().getResultCode(), "预填提交必须成功");
	}

	private static void waitSplittingApplied(Zeze.Dbh2.Dbh2 leader, boolean expectNull, String message)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + 5_000;
		while ((leader.getStateMachine().getBucket().getSplittingMeta() == null) == expectNull) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	/**
	 * ①自指形态（红）：CreateSplitBucket返回指向本桶的条目（resume到指向自身的陈旧条目，
	 * 正是(B)自搬运形态）。守卫必须中止：不发LogSetSplittingMeta（splittingMeta保持null，
	 * 桶保持完整服务）；每120s一轮的createSplitBucket重试噪声是正确的拒绝。
	 * bug：无守卫——LogSetSplittingMeta apply、自身对自身putIfAbsent拷贝、最终endMove把
	 * 源桶数据从data[0]删到尾置死桶，数据物理灭失。
	 */
	@Test
	public void testSelfReferenceAbortsBeforeSetSplittingMeta(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("gad03-log").toString());
		var manager = newStubbedManager(masterAgent);
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			// 源桶[F,L)=[2,8)，6条数据（locateMiddle走keyNumbers/2=3步，中位=5，newMeta=[5,8)）。
			sourceAgent.setBucketMeta(metaOf(key(2), key(8), ""));
			for (int k = 2; k <= 7; ++k)
				put(sourceAgent, k);

			// 自指resume形态：桩返回raftConfig=本桶配置的[5,8)条目。
			leader.tryStartSplit(false);

			Assertions.assertTrue(server.createSplitRequests.get() >= 1,
					"createSplitBucket必须已发出（守卫在其返回后）");
			// 守卫中止：LogSetSplittingMeta不得append（bug时毫秒级apply可见）。
			//noinspection BusyWait
			Thread.sleep(1_000);
			Assertions.assertNull(leader.getStateMachine().getBucket().getSplittingMeta(),
					"自指条目必须中止本轮（bug：LogSetSplittingMeta已apply——自身对自身拷贝后endMove删全量置死桶，数据灭失）");
			// 桶保持完整服务：数据仍在，meta未动。
			Assertions.assertEquals(0, key(2).compareTo(leader.getStateMachine().getBucket().getBucketMeta().getKeyFirst()),
					"中止后源桶meta不得被动");
			var kv = sourceAgent.get("database", "table1", key(5));
			Assertions.assertTrue(kv.getKey(), "中止后桶必须保持完整服务可读");
		} finally {
			sourceAgent.close();
			for (var n : source)
				n.close();
			logDb.close();
			masterAgent.stop();
			server.stop();
			closeManagerQuietly(manager);
		}
	}

	/**
	 * ②非自指形态（绿）：桩返回真实第二桶配置——守卫放行，分桶照常进入并全流程完成
	 *（LogSetSplittingMeta apply可见、数据复制到目标、settle通知回0、splitting消费），
	 * 证明守卫不过度收紧。
	 */
	@Test
	public void testNonSelfTargetProceeds(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.replyRaftConfig = TARGET_RAFT; // 真实目标桶
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var sourceLogDb = new RocksDatabase(tempDir.resolve("gad03-src-log").toString());
		var targetLogDb = new RocksDatabase(tempDir.resolve("gad03-dst-log").toString());
		var manager = newStubbedManager(masterAgent);
		var source = startBucket(manager, sourceLogDb, SOURCE_RAFT, tempDir, "src2");
		var target = startBucket(manager, targetLogDb, TARGET_RAFT, tempDir, "dst2");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		var targetAgent = new Dbh2Agent(TARGET_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			RaftBucketTopologySupport.waitLeader(target);
			sourceAgent.setBucketMeta(metaOf(key(2), key(8), ""));
			for (int k = 2; k <= 7; ++k)
				put(sourceAgent, k);

			leader.tryStartSplit(false);
			waitSplittingApplied(leader, false, "非自指目标必须放行进入分桶（bug：守卫过度收紧）");

			// 全流程完成：源桶收窄[F,M)=[2,5)，数据复制到目标桶，splitting消费。
			var deadline = System.currentTimeMillis() + 30_000;
			while (leader.getStateMachine().getBucket().getSplittingMeta() != null
					|| leader.getStateMachine().getBucket().getBucketMeta().getKeyLast().compareTo(key(5)) != 0) {
				if (System.currentTimeMillis() > deadline)
					Assertions.fail("非自指分桶全流程未完成（收窄未达[F,M)=[2,5)）");
				//noinspection BusyWait
				Thread.sleep(20);
			}
			var targetLeader = RaftBucketTopologySupport.waitLeader(target);
			Assertions.assertEquals(0, key(5).compareTo(
					targetLeader.getStateMachine().getBucket().getBucketMeta().getKeyFirst()), "目标桶meta.keyFirst=M");
			var kv = targetAgent.get("database", "table1", key(5));
			Assertions.assertTrue(kv.getKey(), "目标桶必须可读");
			Assertions.assertNotNull(kv.getValue(), "[M,L)数据必须复制到目标桶");
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
