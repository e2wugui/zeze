package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.LogEndMove;
import Zeze.Dbh2.LogEndSplit;
import Zeze.Dbh2.Master.AbstractMaster;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.IModule;
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
 * FND21 GA-D01 A1回归：pending-settle标志补发settle通知（INV2）。
 * (A)路径：触发settle的唯一来源是append节点内存中的endSplit2回调——不落raft日志、不落盘，
 * leader在commit→apply→invokeCallback之间进程死亡，则源桶侧LogEndSplit已apply、master侧
 * splitting条目与旧主表条目永存、重试任务随进程死亡消失——新键域读永久失败、无自愈。
 * 修复=LogEndSplit/LogEndMove的apply内落pending-settle标志（派生状态，随raft复制/快照）；
 * recoverSplitting在splittingMeta==null且标志非空时经既有endSplit/endMoveWithRetryAsync
 * 幂等补发；重试链终局（rc==0或eSplittingBucketNotFound）追加LogClearPendingSettle清除
 * （apply侧身份匹配，防跨世代倒灌）。
 * 用例：
 * ①split补发：直构"迁移已commit但通知未达"持久态（appendLog LogEndSplit，无回调=进程死亡
 * 形态），反射触发recoverSplitting——EndSplit必须到达桩master；rc=0终局后标志必须被清除。
 * ②move补发：同上（LogEndMove），断言EndMove形态（from==null）与死桶终态不受影响。
 * ③eSplittingBucketNotFound终局同样清除标志（已结算证据方向的收敛闭环）。
 * ④master侧already-settled守卫（补发安全性前提）：主表现存条目与to四元组+raftConfig全等
 * 时返回eSplittingBucketNotFound且不消费在途新世代splitting条目（同四元组不同raftConfig）。
 * serverId 882段（manager.xml ServerId）与raft端口19180-19182段为本用例族预留。
 */
@Fast
public class TestGAD01PendingSettleReissue {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 端口段与既有Dbh2测试错开：TestSplitPutTombstone=19100/19110、GA01=19130-32、GA02=19140-42、
	// GAD02=19150-52、GAR21=19160-65、GAC03=19170-72；本测试=19180-82。
	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19180"/>
				<node Host="127.0.0.1" Port="19181"/>
				<node Host="127.0.0.1" Port="19182"/>
			</raft>
			""";

	// 目标桶只以raftConfig字符串形态出现（补发链路对目标无真实网络需求，桩master终结即闭环）。
	private static final String TARGET_RAFT_CONFIG = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="RaftName">
				<node Host="127.0.0.1" Port="29180" ProxyHost="127.0.0.1" ProxyPort="39180"/>
			</raft>
			""";

	private static final AtomicInteger tid = new AtomicInteger();

	// 桩master：EndSplit按脚本回码（默认0=成功；eSplittingBucketNotFound=已结算终局），
	// EndMove恒回0，记录请求参数。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger endSplitRequests = new AtomicInteger();
		final AtomicInteger endMoveRequests = new AtomicInteger();
		volatile int splitReplyCode = 0;
		volatile List<BBucketMeta.Data> receivedFrom;
		volatile BBucketMeta.Data receivedTo;

		StubMasterService(int port) {
			super("stubMasterFnd21GAD01", stubServerConfig(port));
			setNoProcedure(true);

			var fhSplit = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndSplit.class, EndSplit.TypeId_);
			fhSplit.Factory = EndSplit::new;
			fhSplit.Handle = r -> onEndSplit(r);
			fhSplit.Level = TransactionLevel.None;
			fhSplit.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndSplit.TypeId_, fhSplit);

			var fhMove = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndMove.class, EndMove.TypeId_);
			fhMove.Factory = EndMove::new;
			fhMove.Handle = r -> onEndMove(r);
			fhMove.Level = TransactionLevel.None;
			fhMove.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndMove.TypeId_, fhMove);
		}

		private long onEndSplit(EndSplit r) {
			endSplitRequests.incrementAndGet();
			receivedFrom = List.of(r.Argument.getFrom());
			receivedTo = r.Argument.getTo();
			// 脚本0=真成功（rc必须为0，模块封装0是非零码会触发重试）；非0=模块错误码。
			r.SendResultCode(splitReplyCode == 0 ? 0 : IModule.errorCode(AbstractMaster.ModuleId, splitReplyCode));
			return 0;
		}

		private long onEndMove(EndMove r) {
			endMoveRequests.incrementAndGet();
			receivedTo = r.Argument.getTo();
			r.SendResultCode(0);
			return 0;
		}
	}

	private static Config stubServerConfig(int port) {
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put("stubMasterFnd21GAD01", conf);
		return config;
	}

	private static MasterAgent stubMasterAgent(int port) {
		var conf = new ServiceConf();
		conf.addConnector(new Connector("127.0.0.1", port, true));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, conf);
		return new MasterAgent(config);
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

	private static Dbh2ManagerStub newStubbedManager(MasterAgent stubAgent, String serverId) throws Exception {
		// home在系统临时目录而非@TempDir：manager内部rocks句柄只能反射关闭，
		// 残留不应让JUnit的@TempDir收尾删除失败（TestFnd20GAR21同款取舍）。
		var home = Files.createTempDirectory("fnd21gad01-manager");
		var xml = home.resolve("manager.xml");
		Files.writeString(xml, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<zeze ServerId=\"" + serverId + "\"/>\n");
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

	// 恢复入口=onLeaderReady挂点（Dbh2构造注册recoverSplitting），私有方法反射触发。
	private static void invokeRecoverSplitting(Zeze.Dbh2.Dbh2 leader) throws Exception {
		Method method = Zeze.Dbh2.Dbh2.class.getDeclaredMethod("recoverSplitting");
		method.setAccessible(true);
		method.invoke(leader);
	}

	// pending-settle标志读取（反射接缝，对齐TestFnd20GAC05口径）：旧基线无Bucket.getPendingSettle
	//（GA-D01 A1修复不存在）返回null——测试仍可编译运行，由断言消息显式判红，红因=真实行为差异。
	private static Object pendingSettleOf(Zeze.Dbh2.Dbh2 node) {
		try {
			return Zeze.Dbh2.Bucket.class.getMethod("getPendingSettle")
					.invoke(node.getStateMachine().getBucket());
		} catch (NoSuchMethodException e) {
			return null;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// 等待pendingSettle达到期望形态：expectNull=false等待落盘（apply可见），true等待清除
	//（LogClearPendingSettle apply可见）。循环条件=当前形态不达期望。
	private static void waitPendingSettle(Zeze.Dbh2.Dbh2 leader, boolean expectNull, String message)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while ((pendingSettleOf(leader) == null) != expectNull) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	private static void waitRequests(AtomicInteger counter, int expected, String message) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while (counter.get() < expected) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	/**
	 * ①split补发+rc=0终局清除：appendLog(LogEndSplit)不带回调=原append节点在commit→apply→
	 * 通知之间死亡（(A)形态）——通知链不存在，标志落盘。bug时：recoverSplitting对
	 * splittingMeta==null恒no-op，master永远等不到settle。修复后补发EndSplit；桩master回0
	 * （终局），onSettled追加LogClearPendingSettle，apply清标志。
	 */
	@Test
	public void testSplitPendingSettleReissuedAndClearedOnSuccess(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("gad01-log").toString());
		var manager = newStubbedManager(masterAgent, "882");
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = Fnd19GABucketSupport.waitLeader(source);
			// 源桶[F,L)=[2,8)；LogEndSplit(from=[2,5), to=[5,8)@target)——与endSplit1构造一致。
			setBucketMeta(sourceAgent, key(2), key(8));
			leader.getRaft().appendLog(new LogEndSplit(
					metaOf(key(2), key(5), ""), metaOf(key(5), key(8), TARGET_RAFT_CONFIG)));
			waitPendingSettle(leader, false, "LogEndSplit.apply必须落下pending-settle标志（注入失败）");

			// 死亡模拟完成（无endSplit2回调链），leader-ready恢复。
			invokeRecoverSplitting(leader);
			waitRequests(server.endSplitRequests, 1, "recoverSplitting必须补发EndSplit到master（bug：通知永久丢失）");

			// 桩master回0=终局：onSettled追加清除日志，apply后标志收回。
			waitPendingSettle(leader, true, "settle终局（rc==0）必须经LogClearPendingSettle清除标志");
			Assertions.assertNull(leader.getStateMachine().getBucket().getSplittingMeta(),
					"补发路径不得复活splittingMeta");
			Assertions.assertNotNull(server.receivedFrom, "补发的EndSplit必须携带完整from/to");
			Assertions.assertEquals(0, key(2).compareTo(server.receivedFrom.get(0).getKeyFirst()),
					"补发from.keyFirst=F");
			Assertions.assertEquals(0, key(5).compareTo(server.receivedTo.getKeyFirst()),
					"补发to.keyFirst=M");
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
	 * ②move补发：LogEndMove(to=[2,8)@target)不带回调。move终态（源桶死桶{1},{1}）不受影响，
	 * 补发走EndMove（from==null）。
	 */
	@Test
	public void testMovePendingSettleReissued(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("gad01-log2").toString());
		var manager = newStubbedManager(masterAgent, "882");
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src2");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = Fnd19GABucketSupport.waitLeader(source);
			setBucketMeta(sourceAgent, key(2), key(8));
			leader.getRaft().appendLog(new LogEndMove(metaOf(key(2), key(8), TARGET_RAFT_CONFIG)));
			waitPendingSettle(leader, false, "LogEndMove.apply必须落下pending-settle标志");

			invokeRecoverSplitting(leader);
			waitRequests(server.endMoveRequests, 1, "move形态必须补发EndMove（bug：通知永久丢失）");
			waitPendingSettle(leader, true, "move终局同样清除标志");

			// move终态保持：源桶死桶{1},{1}（补发不改源桶数据面）。
			var meta = leader.getStateMachine().getBucket().getBucketMeta();
			Assertions.assertEquals(0, new Binary(new byte[]{1}).compareTo(meta.getKeyFirst()),
					"move收尾源桶必须保持死桶标记");
			Assertions.assertNull(pendingSettleOf(leader), "标志必须已清除");
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
	 * ③eSplittingBucketNotFound终局（master已结算、仅通知重复）同样触发清除——
	 * 两方向终局的收敛闭环（桩脚本回该码）。
	 */
	@Test
	public void testAlreadySettledTerminalAlsoClears(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.splitReplyCode = AbstractMaster.eSplittingBucketNotFound; // 已结算终局脚本
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("gad01-log3").toString());
		var manager = newStubbedManager(masterAgent, "882");
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src3");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = Fnd19GABucketSupport.waitLeader(source);
			setBucketMeta(sourceAgent, key(2), key(8));
			leader.getRaft().appendLog(new LogEndSplit(
					metaOf(key(2), key(5), ""), metaOf(key(5), key(8), TARGET_RAFT_CONFIG)));
			waitPendingSettle(leader, false, "标志必须落盘");

			invokeRecoverSplitting(leader);
			waitRequests(server.endSplitRequests, 1, "补发必须到达");
			// 桩对keyFirst非空的to回eSplittingBucketNotFound（已结算证据）——终局同样清除。
			waitPendingSettle(leader, true,
					"eSplittingBucketNotFound终局必须清除标志（不清除则数个选举周期后可跨世代倒灌）");
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

	// —— master侧already-settled守卫（补发的安全性前提）——

	@SuppressWarnings("unchecked")
	private static MasterDatabase getDatabase(Master master) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterDatabase>)field.get(master)).get("db1");
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterTable.Data> getSplitting(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterTable.Data>)field.get(db);
	}

	/**
	 * ④补发安全前提：主表同keyFirst现存条目与to四元组+raftConfig全等=已结算过。此刻表内的
	 * 条目是在途新世代条目（同四元组、不同raftConfig），旧迁移的补发必须拒绝且**不消费**它——
	 * 抢占消费会让新迁移永不settle（bug形态）。
	 */
	@Test
	public void testAlreadySettledGuardKeepsNewGenerationEntry(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);

			// master域内表名统一"t1"（本子用例不经raft注入，与上属raft用例的"table1"无关）。
			var settled = masterMeta(key(2), Binary.Empty, "raftB");
			table.getBuckets().put(key(2), settled);
			var newGeneration = masterMeta(key(2), Binary.Empty, "raftC");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(2), newGeneration);

			// 旧迁移（to=raftB）的补发：四元组匹配通过，主表全等=已结算。
			var r = new EndMove();
			r.Argument.setTo(masterMeta(key(2), Binary.Empty, "raftB"));
			Assertions.assertEquals(master.errorCode(AbstractMaster.eSplittingBucketNotFound), db.endMove(r),
					"已结算的补发必须返回终局码停止重试");

			// 关键断言：不得消费在途新世代条目。
			Assertions.assertSame(newGeneration, getSplitting(db).get("t1").getBuckets().get(key(2)),
					"already-settled守卫不得消费在途新世代splitting条目（bug：被旧迁移补发抢占，新迁移永不settle）");
		} finally {
			master.close();
		}
	}

	private static BBucketMeta.Data masterMeta(Binary keyFirst, Binary keyLast, String raftConfig) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("db1");
		meta.setTableName("t1");
		meta.setRaftConfig(raftConfig);
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		return meta;
	}
}
