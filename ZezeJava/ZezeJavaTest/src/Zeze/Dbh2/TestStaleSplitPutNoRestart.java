package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Master.CheckFreeManager;
import Zeze.Builtin.Dbh2.SplitPut;
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
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksIterator;

/**
 * FND21 GA-C03回归：splitPutNext身份失配分支不得以陈旧上下文重试startSplit。
 * bug：失配分支对hasError无差别重试——endSplit2先dbh2Splitting.close()后置null，
 * Agent.stop()把悬挂中的旧轮SplitPut以Procedure.Timeout同步触发回调（此刻serialNo已
 * 被重入的新轮递增），回调进失配分支仍执行startSplit(isMove)：分桶已完结时（splitting
 * ==null、LogEndSplit已apply）走全新prepare段——再建桶、全量拷贝、再走一轮完整收尾，
 * 一次无人决策的自发重组织；新轮进行中时还会提前drain正阻塞的prepareQueue，扩大扰动。
 * 修复=失配分支仅当回调仍代表当前轮（本机leader且serialNo==splitSerialNo）才重试；
 * serialNo失配的迟到回调只清理迭代器，重试职责属于新轮/loadMonitor。
 * 观测锚：startSplit入口第一动作即递增splitSerialNo——失配回调是否触发了新一轮直接可判。
 * 反向钉住：identity有效（serialNo==当前）但dbh2Splitting==null的hasError回调保留
 * 原重试语义（不过度收紧）。
 * serverId 881段（manager.xml ServerId）与raft端口19170-19172段为本用例预留。
 * 形态：进程内3节点raft桶+回环桩master（CheckFreeManager回count=0，使误入的startSplit
 * 在prepare段即中止——观测集中、无建桶副作用）。
 */
@Fast
public class TestStaleSplitPutNoRestart {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 端口段错开既有Dbh2测试：TestSplitPutTombstone=19100/19110、GA01=19130-32、GA02=19140-42、
	// GAD02=19150-52、GAR21=19160-65；本测试源桶=19170-72。
	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19170"/>
				<node Host="127.0.0.1" Port="19171"/>
				<node Host="127.0.0.1" Port="19172"/>
			</raft>
			""";

	private static final AtomicInteger tid = new AtomicInteger();

	// 桩master：CheckFreeManager恒回count=0——误入startSplit的prepare段在此warn中止，
	// 请求计数即"startSplit被触发"的直接观测。rc=0不留重试定时器。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger checkFreeManagerRequests = new AtomicInteger();

		StubMasterService(int port) {
			super("stubMasterStaleSplit", stubServerConfig(port));
			setNoProcedure(true);

			var fh = new Zeze.Net.Service.ProtocolFactoryHandle<>(CheckFreeManager.class, CheckFreeManager.TypeId_);
			fh.Factory = CheckFreeManager::new;
			fh.Handle = this::onCheckFreeManager;
			fh.Level = TransactionLevel.None;
			fh.Mode = DispatchMode.Normal;
			AddFactoryHandle(CheckFreeManager.TypeId_, fh);
		}

		private long onCheckFreeManager(CheckFreeManager r) {
			checkFreeManagerRequests.incrementAndGet();
			r.Result.setCount(0); // 0 < raftClusterCount(默认3)：startSplit在prepare段warn并return
			r.SendResult();
			return 0;
		}
	}

	private static Config stubServerConfig(int port) {
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put("stubMasterStaleSplit", conf);
		return config;
	}

	private static Config stubClientConfig(int port) {
		var conf = new ServiceConf();
		conf.addConnector(new Connector("127.0.0.1", port, true));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, conf);
		return config;
	}

	// 复用Dbh2Manager实例但getMasterAgent指向桩连接（不start，无端口/定时器副作用）。
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

	private static Dbh2ManagerStub newStubbedManager(Path tempDir, MasterAgent stubAgent) throws Exception {
		// home在系统临时目录而非@TempDir：manager内部rocks句柄只能反射关闭，
		// 残留不应让JUnit的@TempDir收尾删除失败（TestFnd20GAR21同款取舍）。
		var home = Files.createTempDirectory("stale-split-manager");
		var xml = home.resolve("manager.xml");
		Files.writeString(xml, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<zeze ServerId=\"881\"/>\n");
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

	private static void setBucketMeta(Dbh2Agent agent, Binary keyFirst, Binary keyLast) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table1");
		meta.setRaftConfig("");
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		agent.setBucketMeta(meta);
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

	private static long getSplitSerialNo(Zeze.Dbh2.Dbh2 dbh2) throws Exception {
		var field = Zeze.Dbh2.Dbh2.class.getDeclaredField("splitSerialNo");
		field.setAccessible(true);
		return field.getLong(dbh2);
	}

	private static void setSplitSerialNo(Zeze.Dbh2.Dbh2 dbh2, long value) throws Exception {
		var field = Zeze.Dbh2.Dbh2.class.getDeclaredField("splitSerialNo");
		field.setAccessible(true);
		field.setLong(dbh2, value);
	}

	// 模拟endSplit2的Agent.stop()触发形态：悬挂SplitPut以Timeout完结（resultCode非0）。
	private static SplitPut timedOutSplitPut() {
		var r = new SplitPut();
		r.setResultCode(Procedure.Timeout);
		return r;
	}

	private static RocksIterator newIterator(Zeze.Dbh2.Dbh2 leader) {
		return leader.getStateMachine().getBucket().getData().iterator();
	}

	/**
	 * 本案（红）：旧轮迟到回调（serialNo失配+Timeout）不得触发新一轮startSplit。
	 * endSplit2.close()触发的旧轮回调正是这个形态——重试职责属于已接管的新轮。
	 * 观测：splitSerialNo保持不变且startSplit未进prepare段（CheckFreeManager计数为0）。
	 */
	@Test
	public void testStaleSerialNoErrorCallbackDoesNotRestart(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = new MasterAgent(stubClientConfig(port));
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("gac03-log").toString());
		var manager = newStubbedManager(tempDir, masterAgent);
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			setBucketMeta(sourceAgent, key(2), key(8));
			put(sourceAgent, 3);
			put(sourceAgent, 5);

			// 直构"新轮已接管"状态：splitSerialNo=100（重入的startSplit已递增），
			// 旧轮（serialNo=99）悬挂的SplitPut此刻被close()以Timeout触发。
			setSplitSerialNo(leader, 100);
			var before = getSplitSerialNo(leader);
			Assertions.assertEquals(0, leader.splitPutNext(false, timedOutSplitPut(), newIterator(leader), 99),
					"失配回调正常返回0");

			// bug时：失配+hasError无差别startSplit——serialNo被递增、prepare段打到桩master。
			Assertions.assertEquals(before, getSplitSerialNo(leader),
					"serialNo失配的迟到回调不得触发新一轮startSplit（bug：以陈旧上下文发起无人决策的全新分桶轮）");
			Assertions.assertEquals(0, server.checkFreeManagerRequests.get(),
					"迟到回调不得进入startSplit的prepare段（bug：误触checkFreeManager）");
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
	 * 反向钉住（绿）：身份有效（serialNo==当前轮）的hasError回调保留重试语义。
	 * dbh2Splitting==null但serialNo未失配——本轮上下文仍有效，按原语义startSplit重试
	 * （prepare段被桩master的count=0拦下，观测点=serialNo递增），修复不得过度收紧。
	 */
	@Test
	public void testCurrentSerialNoErrorCallbackStillRestarts(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = new MasterAgent(stubClientConfig(port));
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("gac03-log2").toString());
		var manager = newStubbedManager(tempDir, masterAgent);
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src2");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			setBucketMeta(sourceAgent, key(2), key(8));
			put(sourceAgent, 4);

			setSplitSerialNo(leader, 100);
			var before = getSplitSerialNo(leader);
			Assertions.assertEquals(0, leader.splitPutNext(false, timedOutSplitPut(), newIterator(leader), 100),
					"身份有效回调正常返回0");

			// 保留原语义：本机leader且serialNo==当前时hasError必须重试startSplit
			//（splitSerialNo被startSplit重取自manager.atomicSerialNo，断言只看"发生变化"）。
			Assertions.assertNotEquals(before, getSplitSerialNo(leader),
					"身份有效（serialNo==当前轮）的hasError回调必须保持重试语义");
			waitUntil(() -> server.checkFreeManagerRequests.get() >= 1, 10_000,
					"重试的startSplit必须进入prepare段（checkFreeManager已发出）");
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

	private static void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs, String message)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}
}
