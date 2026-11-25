package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Master.BRegister;
import Zeze.Builtin.Dbh2.Master.CheckFreeManager;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * 分桶/迁移重建目标桶 Raft 拓扑时必须清空源桶的 host2Raft 映射：
 * 1) MasterDatabase.buildRaftConfig 仅保留本次新选中的 manager 映射，多 manager 集群中排除在外的
 *    源 manager 不得残留于目标桶 host2Raft（否则孤儿回收会向源 manager 发出携带源桶 portId 的 DestroyBucket，
 *    且源 manager 重注册时会收到指向目标节点集的伪造 raft.xml）。
 * 2) Dbh2.startSplit 构造 CreateSplitBucket 请求时同步清空 newMeta.host2Raft，不向上游携带源拓扑。
 */
@Fast
public class TestSplitBucketClearsSourceHost2Raft {

	private static final class ObservedMaster extends Master {
		final List<String> destroyLookups = new CopyOnWriteArrayList<>();

		ObservedMaster(Path home) throws Exception {
			super(home.toString(), new Config());
		}

		@Override
		public @Nullable Manager findManagerByIdentity(String managerHostPort) {
			destroyLookups.add(managerHostPort);
			return null;
		}

		long invokeRegister(Register r) throws Exception {
			return ProcessRegisterRequest(r);
		}
	}

	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicReference<BBucketMeta.@Nullable Data> capturedSplitRequest = new AtomicReference<>();

		StubMasterService(int port) {
			super("stubMasterSplitHost2Raft", stubServerConfig(port));
			setNoProcedure(true);

			var checkFh = new Zeze.Net.Service.ProtocolFactoryHandle<>(CheckFreeManager.class, CheckFreeManager.TypeId_);
			checkFh.Factory = CheckFreeManager::new;
			checkFh.Handle = r -> {
				r.Result.setCount(3);
				r.SendResult();
				return 0;
			};
			checkFh.Level = TransactionLevel.None;
			checkFh.Mode = DispatchMode.Normal;
			AddFactoryHandle(CheckFreeManager.TypeId_, checkFh);

			var createFh = new Zeze.Net.Service.ProtocolFactoryHandle<>(CreateSplitBucket.class, CreateSplitBucket.TypeId_);
			createFh.Factory = CreateSplitBucket::new;
			createFh.Handle = r -> {
				capturedSplitRequest.set(r.Argument.copy());
				r.SendResultCode(Zeze.IModule.errorCode(Zeze.Dbh2.Master.AbstractMaster.ModuleId, Master.eTooFewManager));
				return 0;
			};
			createFh.Level = TransactionLevel.None;
			createFh.Mode = DispatchMode.Normal;
			AddFactoryHandle(CreateSplitBucket.TypeId_, createFh);
		}
	}

	private static Config stubServerConfig(int port) {
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put("stubMasterSplitHost2Raft", conf);
		return config;
	}

	private static Config stubClientConfig(int port) {
		var conf = new ServiceConf();
		conf.addConnector(new Connector("127.0.0.1", port, true));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, conf);
		return config;
	}

	private static final class Dbh2ManagerStub extends Dbh2Manager {
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

	private static Master.Manager newManager(String host, int port) {
		var reg = new BRegister.Data();
		reg.setDbh2RaftAcceptorName(host);
		reg.setPort(port);
		var m = new Master.Manager(Mockito.mock(AsyncSocket.class), reg);
		m.ready = true;
		return m;
	}

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<String> invokeBuildRaftConfig(MasterDatabase db, BBucketMeta.Data bucket,
														   ArrayList<Master.Manager> managers) throws Exception {
		Method method = MasterDatabase.class.getDeclaredMethod("buildRaftConfig", BBucketMeta.Data.class, ArrayList.class);
		method.setAccessible(true);
		return (ArrayList<String>)method.invoke(db, bucket, managers);
	}

	@SuppressWarnings("unchecked")
	private static MasterTable.Data splittingTable(MasterDatabase db, String tableName) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterTable.Data>)field.get(db))
				.computeIfAbsent(tableName, __ -> new MasterTable.Data());
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterDatabase> masterDatabases(Master master) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterDatabase>)field.get(master);
	}

	private static int[] allocatePorts(int count) throws Exception {
		var sockets = new ArrayList<ServerSocket>(count);
		try {
			var ports = new int[count];
			for (int i = 0; i < count; ++i) {
				var ss = new ServerSocket(0);
				sockets.add(ss);
				ports[i] = ss.getLocalPort();
			}
			return ports;
		} finally {
			for (var ss : sockets)
				ss.close();
		}
	}

	@Test
	void testBuildRaftConfigClearsSourceHost2RaftAcrossManagers(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("db1"));
		var master = new ObservedMaster(tempDir);
		try {
			var db = requireNonNull(masterDatabases(master).get("db1"));

			var m1 = newManager("127.0.0.1", 21001);
			var m2 = newManager("127.0.0.1", 21002);
			var m3 = newManager("127.0.0.1", 21003);
			var m4 = newManager("127.0.0.1", 21004);

			// 源桶落在 {M1, M2, M3}。
			var source = new BBucketMeta.Data();
			source.setDatabaseName("db1");
			source.setTableName("t1");
			source.setKeyFirst(Binary.Empty);
			source.setKeyLast(Binary.Empty);
			var sourceManagers = new ArrayList<>(List.of(m1, m2, m3));
			invokeBuildRaftConfig(db, source, sourceManagers);
			Assertions.assertEquals(Set.of("127.0.0.1_21001", "127.0.0.1_21002", "127.0.0.1_21003"),
					source.getHost2Raft().keySet());

			// 目标桶从源桶 copy 构造（携带旧 host2Raft），但调度到 {M2, M3, M4}。
			var target = source.copy();
			target.setRaftConfig("");
			target.setKeyFirst(key(5));
			var targetManagers = new ArrayList<>(List.of(m2, m3, m4));
			var targetRaftNames = invokeBuildRaftConfig(db, target, targetManagers);

			Assertions.assertEquals(Set.of("127.0.0.1_21002", "127.0.0.1_21003", "127.0.0.1_21004"),
					target.getHost2Raft().keySet(),
					"buildRaftConfig 必须清空源桶 host2Raft，不得残留被排除的源 manager M1");
			Assertions.assertFalse(target.getHost2Raft().containsKey("127.0.0.1_21001"));
			Assertions.assertEquals(targetRaftNames.get(0), target.getHost2Raft().get("127.0.0.1_21002"));
			Assertions.assertEquals(targetRaftNames.get(1), target.getHost2Raft().get("127.0.0.1_21003"));
			Assertions.assertEquals(targetRaftNames.get(2), target.getHost2Raft().get("127.0.0.1_21004"));

			// 验证孤儿回收：若 target 成为死 splitting 条目，scanSplittingAge 绝不触碰源 manager M1。
			var main = new MasterTable.Data();
			db.getTables().put("t1", main);
			main.getBuckets().put(Binary.Empty, source.copy());
			splittingTable(db, "t1").getBuckets().put(key(5), target.copy());

			master.getDbh2Config().setSplittingAgeWarnMs(0);
			Assertions.assertEquals(0, db.scanSplittingAge(), "首扫登记世代");

			// 模拟源桶收窄到 [Empty, k3)，使 target [k5, Empty) 成为旧世代死条目。
			requireNonNull(main.getBuckets().get(Binary.Empty)).setKeyLast(key(3));
			Assertions.assertEquals(1, db.scanSplittingAge(), "超龄死条目触发回收尝试");
			Assertions.assertFalse(master.destroyLookups.contains("127.0.0.1_21001"),
					"孤儿回收不得向活源桶独有的 manager M1 发 DestroyBucket");
			Assertions.assertEquals(Set.of("127.0.0.1_21002", "127.0.0.1_21003", "127.0.0.1_21004"),
					Set.copyOf(master.destroyLookups));

			// 验证结算后 M1 重注册不会收到 target 的非法 raftConfig。
			requireNonNull(main.getBuckets().get(Binary.Empty)).setKeyLast(Binary.Empty);
			var settledFrom = source.copy();
			settledFrom.setKeyLast(key(5));
			var endSplit = new EndSplit();
			endSplit.Argument.setFrom(settledFrom);
			endSplit.Argument.setTo(target.copy());
			Assertions.assertEquals(0, db.endSplit(endSplit));

			var regRpc = new Register() {
				@Override
				public void SendResult(@Nullable Binary result) {
				}
			};
			regRpc.setSender(Mockito.mock(AsyncSocket.class));
			regRpc.Argument.setDbh2RaftAcceptorName("127.0.0.1");
			regRpc.Argument.setPort(21001);
			Assertions.assertEquals(0, master.invokeRegister(regRpc));
			Assertions.assertEquals(1, regRpc.Result.getDbh2Configs().size(),
					"M1 重注册只应收到它所属的源桶配置，不得收到迁往 {M2,M3,M4} 的目标桶配置");
			var returnedRaft = RaftConfig.loadFromString(regRpc.Result.getDbh2Configs().get(0).getRaftConfig());
			var returnedName = requireNonNull(returnedRaft.getName());
			Assertions.assertNotNull(returnedRaft.getNodes().get(returnedName),
					"返回给 M1 的 raftConfig 必须包含其自身节点名");
		} finally {
			master.close();
		}
	}

	@Test
	void testStartSplitClearsHost2RaftBeforeRpc(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var ports = allocatePorts(4);
		var masterPort = ports[0];
		var raftConfig = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(ports[1], ports[2], ports[3]);

		var server = new StubMasterService(masterPort);
		server.start();
		var masterAgent = new MasterAgent(stubClientConfig(masterPort));
		masterAgent.startAndWaitConnectionReady();
		var logDb = new RocksDatabase(tempDir.resolve("split-clear-host2raft-log").toString());
		var managerHome = Files.createTempDirectory("split-clear-host2raft-mgr");
		var managerXml = managerHome.resolve("manager.xml");
		Files.writeString(managerXml, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<zeze ServerId=\"0\"/>\n");
		var manager = new Dbh2ManagerStub(managerHome, managerXml.toString(), masterAgent);
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = new ArrayList<Dbh2>();
		Dbh2Agent agent = null;
		try {
			for (var nodeConf : RaftConfig.loadFromString(raftConfig).getNodes().values()) {
				var nodeXml = raftConfig.replaceFirst("<raft ",
						"<raft DbHome=\"" + java.util.regex.Matcher.quoteReplacement(
								tempDir.resolve("node_" + nodeConf.getName().replace(':', '_')).toString()) + "\" ");
				nodes.add(new Dbh2(manager, nodeConf.getName(), logDb,
						RaftConfig.loadFromString(nodeXml), null, taskOneByOne));
			}
			agent = new Dbh2Agent(raftConfig);
			var leader = RaftBucketTopologySupport.waitLeader(nodes);

			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("db1");
			meta.setTableName("t1");
			meta.setRaftConfig(raftConfig);
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			meta.getHost2Raft().put("127.0.0.1_21001", "127.0.0.1_10001");
			meta.getHost2Raft().put("127.0.0.1_21002", "127.0.0.1_10002");
			meta.getHost2Raft().put("127.0.0.1_21003", "127.0.0.1_10003");
			agent.setBucketMeta(meta);

			for (int i = 1; i <= 4; ++i) {
				var batch = new BPrepareBatch.Data("", "db1", "t1", null);
				batch.getBatch().setTid(i);
				batch.getBatch().getPuts().put(key(i), key(i + 10));
				var f = agent.prepareBatch(batch);
				f.await();
				Assertions.assertEquals(0, f.get().getResultCode());
				Assertions.assertEquals(0, agent.commitBatch(i).await().get().getResultCode());
			}

			try {
				leader.tryStartSplit(false);
			} catch (RuntimeException ignored) {
				// StubMasterService 返回 eTooFewManager 终止本轮，仅校验发出的请求参数。
			}

			var captured = server.capturedSplitRequest.get();
			Assertions.assertNotNull(captured, "startSplit 必须发出 CreateSplitBucket 请求");
			Assertions.assertTrue(requireNonNull(captured).getHost2Raft().isEmpty(),
					"startSplit 发出 CreateSplitBucket 前必须清空 newMeta.host2Raft，不得携带源桶拓扑");
		} finally {
			if (agent != null)
				agent.close();
			for (var n : nodes)
				n.close();
			logDb.close();
			masterAgent.stop();
			server.stop();
			try {
				var field = Dbh2Manager.class.getDeclaredField("database");
				field.setAccessible(true);
				((RocksDatabase)field.get(manager)).close();
			} catch (Exception ignored) {
				// best effort
			}
			LogSequence.deleteDirectory(manager.home.toFile());
		}
	}
}
