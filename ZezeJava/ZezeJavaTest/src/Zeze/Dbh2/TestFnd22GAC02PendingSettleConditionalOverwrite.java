package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.LogEndMove;
import Zeze.Dbh2.LogEndSplit;
import Zeze.Dbh2.Master.MasterAgent;
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
 * FND22 GA-C02回归：pending-settle标志单槽**条件覆写**（带身份判别）。
 * bug机制：setPendingSettle无条件覆盖旧标志（Bucket.java原实现"pendingSettle=new PendingSettle
 * (from,to)"）。系统不保证"上一次迁移settle完成前不开始下一次"（loadMonitor/tryStartSplit闸门
 * 只查splittingMeta，LogEndSplit apply即删splitting而settle是此后独立的30s重试链）——堆叠迁移
 * （前次迁移settle未完成时又完成一次迁移）的LogEndSplit/LogEndMove apply把旧标志覆盖掉。进程
 * 死亡后旧迁移仅剩的settle来源（标志）指向最新那次，recoverSplitting只补发最新——旧迁移永不
 * 结算，其to键域主表无主、读写永久失败且仅年龄告警。
 * 修复=旧标志未清且属不同迁移（to身份不等）时不覆写（保留旧迁移的死亡恢复源，error留观测
 * 线索）；同身份幂等放行。用例：
 * ①堆叠形态（split后move）：move的LogEndMove apply不得覆盖未清的split标志；进程死亡模拟
 * （无endSplit2回调链）后recoverSplitting补发的必须是**旧split的EndSplit**（bug：补发被
 * 覆写的EndMove），settle终局后标志清除。
 * ②槽位清空后新迁移可正常落标志（条件覆写不是永久闭锁）。
 * serverId 900段（manager.xml ServerId）与raft端口19220-19222段为本用例族预留。
 */
@Fast
public class TestFnd22GAC02PendingSettleConditionalOverwrite {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 端口段与既有Dbh2测试错开：GAD01=19180-82、GAD03=19190-95、GAD05=19200-02、
	// Fnd20GAC02=19210-12；本测试=19220-22。
	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19220"/>
				<node Host="127.0.0.1" Port="19221"/>
				<node Host="127.0.0.1" Port="19222"/>
			</raft>
			""";

	// 目标桶只以raftConfig字符串形态出现（补发链路对目标无真实网络需求，桩master终结即闭环）。
	private static final String TARGET_RAFT_CONFIG = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="RaftName">
				<node Host="127.0.0.1" Port="29220" ProxyHost="127.0.0.1" ProxyPort="39220"/>
			</raft>
			""";

	// move目标（不同raft端口=不同身份，供堆叠形态区分两个迁移）。
	private static final String MOVE_TARGET_RAFT_CONFIG = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="RaftName">
				<node Host="127.0.0.1" Port="29221" ProxyHost="127.0.0.1" ProxyPort="39221"/>
			</raft>
			""";

	// 桩master：EndSplit/EndMove回0，分计数并记录参数。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger endSplitRequests = new AtomicInteger();
		final AtomicInteger endMoveRequests = new AtomicInteger();
		volatile BBucketMeta.Data receivedSplitFrom;
		volatile BBucketMeta.Data receivedSplitTo;
		volatile BBucketMeta.Data receivedMoveTo;

		StubMasterService(int port) {
			super("stubMasterFnd22C02", stubServerConfig(port));
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
			receivedSplitFrom = r.Argument.getFrom();
			receivedSplitTo = r.Argument.getTo();
			r.SendResultCode(0);
			return 0;
		}

		private long onEndMove(EndMove r) {
			endMoveRequests.incrementAndGet();
			receivedMoveTo = r.Argument.getTo();
			r.SendResultCode(0);
			return 0;
		}
	}

	private static Config stubServerConfig(int port) {
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put("stubMasterFnd22C02", conf);
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
		var home = Files.createTempDirectory("fnd22c02-manager");
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

	private static void invokeRecoverSplitting(Zeze.Dbh2.Dbh2 leader) throws Exception {
		Method method = Zeze.Dbh2.Dbh2.class.getDeclaredMethod("recoverSplitting");
		method.setAccessible(true);
		method.invoke(leader);
	}

	// 等待任一计数器达到expected（快失败：另一形态先到即返回，红因聚焦"补发了哪个形态"）。
	private static void waitAnyRequest(AtomicInteger a, AtomicInteger b, int expected, String message)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while (a.get() < expected && b.get() < expected) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	private static void waitFlag(Zeze.Dbh2.Dbh2 leader, boolean expectNull, String message)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while ((leader.getStateMachine().getBucket().getPendingSettle() == null) != expectNull) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	// move收尾apply的观测锚：源桶meta被置死桶{1},{1}。
	private static void waitDeadBucket(Zeze.Dbh2.Dbh2 leader, String message) throws InterruptedException {
		var deadKey = new Binary(new byte[]{1});
		var deadline = System.currentTimeMillis() + 10_000;
		while (!(0 == deadKey.compareTo(leader.getStateMachine().getBucket().getBucketMeta().getKeyFirst())
				&& 0 == deadKey.compareTo(leader.getStateMachine().getBucket().getBucketMeta().getKeyLast()))) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	/**
	 * ①主用例（红）：堆叠形态——split1（from=[2,5), to=[5,8)@T1）apply落标志且settle未终局，
	 * 随后move（to=[2,8)@T2）apply。move的setPendingSettle不得覆盖未清的split标志；进程死亡
	 * 模拟（两条日志都无回调链）后recoverSplitting补发的必须是旧split的EndSplit（bug：标志被
	 * 覆写为move，补发EndMove——旧split的settle永久丢失，[5,8)键域主表无主）。
	 */
	@Test
	public void testStackedMoveKeepsOldSplitFlagForReissue(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("c02-log1").toString());
		var manager = newStubbedManager(masterAgent, "900");
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src1");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = Fnd19GABucketSupport.waitLeader(source);
			setBucketMeta(sourceAgent, key(2), key(8));

			// split1完成apply（settle因master不可达滞留=标志未清）。
			leader.getRaft().appendLog(new LogEndSplit(
					metaOf(key(2), key(5), ""), metaOf(key(5), key(8), TARGET_RAFT_CONFIG)));
			waitFlag(leader, false, "LogEndSplit.apply必须落下pending-settle标志（注入失败）");

			// 同桶随后完成move（loadMonitor闸门只查splittingMeta，此处直接注入第二条日志）。
			leader.getRaft().appendLog(new LogEndMove(metaOf(key(2), key(8), MOVE_TARGET_RAFT_CONFIG)));
			waitDeadBucket(leader, "LogEndMove.apply必须完成（源桶死桶{1},{1}）");

			// 核心断言（红点）：标志必须仍是旧split的（from非null且to=[5,8)@T1），
			// bug时被覆写为move形态（from=null且to=[2,8)@T2）。
			var flag = leader.getStateMachine().getBucket().getPendingSettle();
			Assertions.assertNotNull(flag, "move apply后标志必须存在（旧split的未清标志）");
			Assertions.assertNotNull(flag.getFrom(),
					"未清的split标志不得被move覆写（bug：单槽无条件覆盖灭失旧迁移的补发源）");
			Assertions.assertEquals(0, key(5).compareTo(flag.getTo().getKeyFirst()),
					"标志必须是旧split的to（keyFirst=5）");
			Assertions.assertEquals(TARGET_RAFT_CONFIG, flag.getTo().getRaftConfig(),
					"标志必须是旧split的to（T1身份）");

			// 进程死亡模拟（两条日志均无settle回调链），leader-ready恢复：必须补发旧split的EndSplit。
			invokeRecoverSplitting(leader);
			waitAnyRequest(server.endSplitRequests, server.endMoveRequests, 1,
					"recoverSplitting必须补发settle（bug：堆叠丢失下无任何补发）");
			Assertions.assertEquals(1, server.endSplitRequests.get(),
					"补发的必须是未清的旧split标志（EndSplit）");
			Assertions.assertEquals(0, server.endMoveRequests.get(),
					"被跳过落标志的move不得占用补发（bug：覆写后补发EndMove，旧split永不结算）");
			Assertions.assertEquals(0, key(2).compareTo(server.receivedSplitFrom.getKeyFirst()),
					"补发的EndSplit.from=F");
			Assertions.assertEquals(0, key(5).compareTo(server.receivedSplitTo.getKeyFirst()),
					"补发的EndSplit.to.keyFirst=M");

			// 桩回0=终局：onSettled追加LogClearPendingSettle，apply清标志，收敛闭环。
			waitFlag(leader, true, "旧split补发终局后标志必须清除");
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
	 * ②钉住（绿）：条件覆写不是永久闭锁——旧标志终局清除后，新迁移的LogEndSplit apply照常
	 * 落下自己的标志并被补发（保住正常串行迁移的(A)路径恢复能力）。
	 */
	@Test
	public void testFlagSetAfterOldCleared(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port);
		server.start();
		var masterAgent = stubMasterAgent(port);
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("c02-log2").toString());
		var manager = newStubbedManager(masterAgent, "900");
		var source = startBucket(manager, logDb, SOURCE_RAFT, tempDir, "src2");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = Fnd19GABucketSupport.waitLeader(source);
			setBucketMeta(sourceAgent, key(2), key(8));

			// split1落标志→补发终局→清除。
			leader.getRaft().appendLog(new LogEndSplit(
					metaOf(key(2), key(5), ""), metaOf(key(5), key(8), TARGET_RAFT_CONFIG)));
			waitFlag(leader, false, "split1标志必须落盘");
			invokeRecoverSplitting(leader);
			waitAnyRequest(server.endSplitRequests, server.endMoveRequests, 1, "split1补发必须到达");
			waitFlag(leader, true, "split1终局后标志必须清除");

			// 槽位已空：split2（[2,5)再分裂于3）的标志必须照常落下并可补发。
			leader.getRaft().appendLog(new LogEndSplit(
					metaOf(key(2), key(3), ""), metaOf(key(3), key(5), TARGET_RAFT_CONFIG)));
			waitFlag(leader, false, "槽位清空后新迁移的标志必须照常落下（条件覆写不得永久闭锁）");
			var flag = leader.getStateMachine().getBucket().getPendingSettle();
			Assertions.assertNotNull(flag.getFrom(), "新split标志的from必须非null");
			Assertions.assertEquals(0, key(3).compareTo(flag.getTo().getKeyFirst()), "新split标志的to必须是split2的");
			invokeRecoverSplitting(leader);
			waitAnyRequest(server.endSplitRequests, server.endMoveRequests, 2, "split2补发必须到达");
			waitFlag(leader, true, "split2终局后标志必须清除");
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
}
