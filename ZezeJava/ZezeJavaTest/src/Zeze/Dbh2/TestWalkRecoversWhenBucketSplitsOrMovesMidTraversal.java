package Zeze.Dbh2;

import harness.Extra;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * walk/walkKey 遍历中途撞上桶拓扑变更（分裂收窄源桶meta/迁移置死源桶）时，
 * 全表遍历必须收齐全部键域：服务端把"桶已失效"与"正常桶尾"区分开（收窄/置死
 * 的桶对一切游标形态拒绝），客户端收到拒绝即刷新主表并按当前游标重定位迭代器。
 * 桶拓扑：A(19340-42)初始[Empty,Empty)持有全部数据1..9；T(19350-52)分裂产生的
 * 新桶[5,Empty)；C(19360-62)迁移目标桶[5,Empty)（T置死后接管键域）。
 */
@Extra
public class TestWalkRecoversWhenBucketSplitsOrMovesMidTraversal {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT_A = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19340"/>
				<node Host="127.0.0.1" Port="19341"/>
				<node Host="127.0.0.1" Port="19342"/>
			</raft>
			""";

	private static final String RAFT_T = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19350"/>
				<node Host="127.0.0.1" Port="19351"/>
				<node Host="127.0.0.1" Port="19352"/>
			</raft>
			""";

	private static final String RAFT_C = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19360"/>
				<node Host="127.0.0.1" Port="19361"/>
				<node Host="127.0.0.1" Port="19362"/>
			</raft>
			""";

	// endMove置死桶的meta哨兵（Dbh2StateMachine.endMove同款）：keyFirst=keyLast={1}。
	private static final Binary deadMetaBound = new Binary(new byte[]{1});

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	// 每节点独立loadFromString一份RaftConfig并显式DbHome（见TestDbh2MultiBucketWalk同款说明）。
	private static List<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, String raftConfigString, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static void stopBucket(List<Zeze.Dbh2.Dbh2> nodes) throws Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
	}

	private static void setBucketMeta(Dbh2Agent agent, String db, String table, Binary keyFirst, Binary keyLast) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName(db);
		meta.setTableName(table);
		meta.setRaftConfig("");
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		agent.setBucketMeta(meta);
	}

	private static void put(Dbh2Agent agent, String db, String table, long tid, List<Binary> keys) throws Exception {
		var batch = new BPrepareBatch.Data("", db, table, null);
		for (var k : keys)
			batch.getBatch().getPuts().put(k, new Binary(new byte[]{(byte)1}));
		batch.getBatch().setTid(tid);
		agent.prepareBatch(batch).await();
		agent.commitBatch(tid).await();
	}

	// 阶段化master桩：getBuckets返回当前阶段的主表快照，拓扑变更后由测试切换阶段，
	// 客户端reload由此拿到新表（真实master在settleSplit/settleMove后发布同样的新表）。
	private static final class StagedMasterAgent extends MasterAgent {
		private volatile MasterTable.Data staged;

		StagedMasterAgent(MasterTable.Data initial) {
			super(new Config());
			staged = initial;
		}

		void stage(MasterTable.Data table) {
			staged = table;
		}

		@Override
		public MasterTable.Data getBuckets(String database, String table) {
			return staged;
		}
	}

	private static MasterTable.Data masterTable(BBucketMeta.Data... buckets) {
		var table = new MasterTable.Data();
		for (var meta : buckets)
			table.getBuckets().put(meta.getKeyFirst(), meta);
		return table;
	}

	private static BBucketMeta.Data bucketMeta(String db, String table, String raft, Binary keyFirst, Binary keyLast) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName(db);
		meta.setTableName(table);
		meta.setRaftConfig(raft);
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		return meta;
	}

	// 直驱harness：反射预填私有agents表绕开openBucket的ProxyAgent接线（生产路径
	// openBucket经proxy为raft挂连接；walk分页逻辑本身与openBucket无关）。
	@SuppressWarnings("unchecked")
	private static void registerDirectAgents(Dbh2AgentManager manager, Dbh2Agent... agents) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("agents");
		field.setAccessible(true);
		var map = (ConcurrentHashMap<String, Dbh2Agent>)field.get(manager);
		for (var agent : agents)
			map.put(agent.getRaftConfigString(), agent);
	}

	@Timeout(120)
	@Test
	public void testWalkAndWalkKeyCollectAllKeysAcrossSplitAndMoveDuringTraversal(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("dbh2WalkTopology").toString());
		var nodesA = startBucket(database, RAFT_A, tempDir);
		var nodesT = startBucket(database, RAFT_T, tempDir);
		var nodesC = startBucket(database, RAFT_C, tempDir);
		var agentA = new Dbh2Agent(RAFT_A);
		var agentT = new Dbh2Agent(RAFT_T);
		var agentC = new Dbh2Agent(RAFT_C);
		var manager = new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(),
				Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(tempDir).toString()));
		try {
			var db = "database";
			var table = "table1";
			var tid = new long[1];

			setBucketMeta(agentA, db, table, Binary.Empty, Binary.Empty);
			put(agentA, db, table, ++tid[0], expectedKeys(1, 9));
			var master = new StagedMasterAgent(masterTable(
					bucketMeta(db, table, RAFT_A, Binary.Empty, Binary.Empty)));
			registerDirectAgents(manager, agentA, agentT, agentC);

			// ---- 阶段一：分页walk中途完成分裂（A收窄为[Empty,5)，新桶T=[5,Empty)接管[5,∞)）。
			var keys = new ArrayList<Binary>();
			ByteBuffer cursor = manager.walk(master, "testMaster", db, table, null, 3,
					(k, v) -> keys.add(new Binary(k)), false, null);
			Assertions.assertEquals(expectedKeys(1, 3), keys, "页1应停在桶A中部（游标形态：桶内非空游标）");
			// 分裂完结（等价endSplit的apply效果：源桶meta收窄；数据已复制进新桶）。
			setBucketMeta(agentA, db, table, Binary.Empty, key(5));
			setBucketMeta(agentT, db, table, key(5), Binary.Empty);
			put(agentT, db, table, ++tid[0], expectedKeys(5, 9));
			master.stage(masterTable(
					bucketMeta(db, table, RAFT_A, Binary.Empty, key(5)),
					bucketMeta(db, table, RAFT_T, key(5), Binary.Empty)));
			int rounds = 0;
			while (cursor != null && ++rounds < 100)
				cursor = manager.walk(master, "testMaster", db, table, cursor, 3,
						(k, v) -> keys.add(new Binary(k)), false, null);
			Assertions.assertEquals(expectedKeys(1, 9), keys,
					"分裂于遍历中途完成时全表walk必须收齐全部键域（新桶键域不得被静默跳过）");

			// ---- 阶段二：分页walkKey中途完成迁移（T置死，C=[5,Empty)接管同一键域）。
			var keys2 = new ArrayList<Binary>();
			ByteBuffer cursor2 = manager.walkKey(master, "testMaster", db, table, null, 3,
					k -> keys2.add(new Binary(k)), false, null);
			Assertions.assertEquals(expectedKeys(1, 3), keys2, "walkKey页1应停在桶A中部");
			// 迁移完结（等价endMove的apply效果：源桶meta置死{1},{1}；数据已复制进目标桶）。
			setBucketMeta(agentT, db, table, deadMetaBound, deadMetaBound);
			setBucketMeta(agentC, db, table, key(5), Binary.Empty);
			put(agentC, db, table, ++tid[0], expectedKeys(5, 9));
			master.stage(masterTable(
					bucketMeta(db, table, RAFT_A, Binary.Empty, key(5)),
					bucketMeta(db, table, RAFT_C, key(5), Binary.Empty)));
			int rounds2 = 0;
			while (cursor2 != null && ++rounds2 < 100)
				cursor2 = manager.walkKey(master, "testMaster", db, table, cursor2, 3,
						k -> keys2.add(new Binary(k)), false, null);
			Assertions.assertEquals(expectedKeys(1, 9), keys2,
					"迁移于遍历中途完成时全表walkKey必须收齐全部键域（置死桶不得被当正常桶尾跳过键域）");
		} finally {
			manager.stop();
			stopBucket(nodesA);
			stopBucket(nodesT);
			stopBucket(nodesC);
			database.close();
		}
	}

	private static List<Binary> expectedKeys(int from, int to) {
		var keys = new ArrayList<Binary>();
		if (from <= to)
			for (var i = from; i <= to; ++i)
				keys.add(key(i));
		else
			for (var i = from; i >= to; --i)
				keys.add(key(i));
		return keys;
	}
}
