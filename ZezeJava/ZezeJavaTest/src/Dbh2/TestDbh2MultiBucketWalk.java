package Dbh2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.Dbh2AgentManager;
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
import org.junit.jupiter.api.io.TempDir;

/**
 * 多桶表的 walk 语义（Dbh2AgentManager）：
 * 分页版 walk/walkKey 到桶尾不能终止（要继续下一个桶），desc 方向的桶定位/迭代要倒序；
 * 全表版 walk/walkKey 的 desc 要按 keyFirst 降序迭代桶。
 * 桶拓扑：A=[Empty,5) 端口19110-19112，B=[5,Empty) 端口19120-19122，数据key 1..9。
 */
public class TestDbh2MultiBucketWalk {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT_A = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19110"/>
				<node Host="127.0.0.1" Port="19111"/>
				<node Host="127.0.0.1" Port="19112"/>
			</raft>
			""";

	private static final String RAFT_B = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19120"/>
				<node Host="127.0.0.1" Port="19121"/>
				<node Host="127.0.0.1" Port="19122"/>
			</raft>
			""";

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	// 每个节点独立loadFromString一份RaftConfig（Raft构造会改写配置对象，共享导致节点身份错乱）；
	// 显式设置DbHome后Raft不再按节点名在cwd下建目录，所有节点目录落在tempDir下，由@TempDir统一清理。
	// RAFT_A/RAFT_B 原样用于agent与bucket meta（桶身份=节点集合，与DbHome无关）。
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

	private static void stopBucket(List<Zeze.Dbh2.Dbh2> nodes, Dbh2Agent agent) throws IOException, Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			// 目录最终由@TempDir清理；这里close后先删一次，给Windows下句柄延迟释放留出重试缓冲，
			// 降低JUnit收尾删除失败把测试搞红的概率。
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
		agent.close();
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

	// 生产manager的openBucket走ProxyAgent（需要Dbh2Manager为raft挂ProxyServer）。
	// 这里用直连agent预填私有agents表，绕开proxy接线——walk逻辑本身与openBucket无关。
	@SuppressWarnings("unchecked")
	private static void registerDirectAgents(Dbh2AgentManager manager, String raftA, Dbh2Agent agentA,
											 String raftB, Dbh2Agent agentB) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("agents");
		field.setAccessible(true);
		var agents = (ConcurrentHashMap<String, Dbh2Agent>)field.get(manager);
		agents.put(raftA, agentA);
		agents.put(raftB, agentB);
	}

	@Test
	public void testMultiBucketWalk(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("dbh2TestMultiBucketWalk").toString());
		var nodesA = startBucket(database, RAFT_A, tempDir);
		var nodesB = startBucket(database, RAFT_B, tempDir);
		var agentA = new Dbh2Agent(RAFT_A);
		var agentB = new Dbh2Agent(RAFT_B);
		var serviceManager = Application.createServiceManager(Zeze.Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();
		// CommitRocks home 重定向到临时目录（默认会落在cwd下CommitRocks{serverId}）。
		System.setProperty("Dbh2CommitRocksHome", tempDir.resolve("CommitRocks").toString());
		var manager = new Dbh2AgentManager(serviceManager, null, 103);
		try {
			var db = "database";
			var table = "table1";

			setBucketMeta(agentA, db, table, Binary.Empty, key(5));
			setBucketMeta(agentB, db, table, key(5), Binary.Empty);
			put(agentA, db, table, 1, List.of(key(1), key(2), key(3), key(4)));
			put(agentB, db, table, 2, List.of(key(5), key(6), key(7), key(8), key(9)));

			var tableData = new MasterTable.Data();
			var metaA = new BBucketMeta.Data();
			metaA.setDatabaseName(db);
			metaA.setTableName(table);
			metaA.setRaftConfig(RAFT_A);
			metaA.setKeyFirst(Binary.Empty);
			metaA.setKeyLast(key(5));
			var metaB = new BBucketMeta.Data();
			metaB.setDatabaseName(db);
			metaB.setTableName(table);
			metaB.setRaftConfig(RAFT_B);
			metaB.setKeyFirst(key(5));
			metaB.setKeyLast(Binary.Empty);
			tableData.getBuckets().put(Binary.Empty, metaA);
			tableData.getBuckets().put(key(5), metaB);
			manager.putBuckets(tableData, "testMaster", db, table);
			registerDirectAgents(manager, RAFT_A, agentA, RAFT_B, agentB);

			// 全表版 asc：跨桶升序收齐（回归保护，当前应绿）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "testMaster", db, table,
						(k, v) -> keys.add(new Binary(k)), false, null);
				Assertions.assertEquals(9, count);
				Assertions.assertEquals(expectedKeys(1, 9), keys);
			}
			// 全表版 desc：全局降序（当前红：桶间升序）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "testMaster", db, table,
						(k, v) -> keys.add(new Binary(k)), true, null);
				Assertions.assertEquals(9, count);
				Assertions.assertEquals(expectedKeys(9, 1), keys);
			}
			// 分页版 asc：循环到null收齐全部（当前红：桶A尾部即返回null，丢桶B）。
			{
				var keys = new ArrayList<Binary>();
				ByteBuffer cursor = null;
				int rounds = 0;
				do {
					cursor = manager.walk(null, "testMaster", db, table, cursor, 2,
							(k, v) -> keys.add(new Binary(k)), false, null);
				} while (cursor != null && ++rounds < 100);
				Assertions.assertEquals(expectedKeys(1, 9), keys);
			}
			// 分页版 desc：循环到null收齐全局降序（当前红：定位方向错+桶尾终止）。
			{
				var keys = new ArrayList<Binary>();
				ByteBuffer cursor = null;
				int rounds = 0;
				do {
					cursor = manager.walk(null, "testMaster", db, table, cursor, 3,
							(k, v) -> keys.add(new Binary(k)), true, null);
				} while (cursor != null && ++rounds < 100);
				Assertions.assertEquals(expectedKeys(9, 1), keys);
			}
			// 分页版 walkKey asc：空桶场景外也验证key-only路径跨桶。
			{
				var keys = new ArrayList<Binary>();
				ByteBuffer cursor = null;
				int rounds = 0;
				do {
					cursor = manager.walkKey(null, "testMaster", db, table, cursor, 4,
							k -> keys.add(new Binary(k)), false, null);
				} while (cursor != null && ++rounds < 100);
				Assertions.assertEquals(expectedKeys(1, 9), keys);
			}
			// 全表版 walkKey desc（当前红）。
			{
				var keys = new ArrayList<Binary>();
				manager.walkKey(null, "testMaster", db, table, k -> keys.add(new Binary(k)), true, null);
				Assertions.assertEquals(expectedKeys(9, 1), keys);
			}
		} finally {
			manager.stop();
			stopBucket(nodesA, agentA);
			stopBucket(nodesB, agentB);
			database.close();
			System.clearProperty("Dbh2CommitRocksHome");
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
