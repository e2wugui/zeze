package Zeze.Dbh2;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Config;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action3;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GA-C02回归：walk/walkDesc的prefix定位与过滤一致。bug时prefix只用于过滤不用于定位：
 * asc空游标seekToFirst落在更小前缀key上、desc空游标seekForPrev(keyLast)/seekToLast落在更大
 * 前缀key上，首个循环即判定桶尾，多前缀共存桶（Dbh2PrefixTable的既定场景）的目标前缀记录
 * 被静默跳过；desc分页空起点游标=裸4字节prefix，seekForPrev落在前缀区间下方，恒返回空。
 * 拓扑：单桶[Empty,Empty)端口19210-19212，数据=prefix(id1)×2、prefix(id2)×3、prefix(id3)×1。
 * 形态：进程内raft桶（对齐TestDbh2MultiBucketWalk）+NullServiceAgent/远程提交配置
 * （对齐TestFnd19GAD04，@Fast车道无外部ServiceManager依赖）。
 */
@Fast
public class TestPrefixWalkPositioning {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19210"/>
				<node Host="127.0.0.1" Port="19211"/>
				<node Host="127.0.0.1" Port="19212"/>
			</raft>
			""";

	// Dbh2PrefixTable同款key布局：4字节LE表id + 用户key。
	private static Binary pkey(int id, int userKey) {
		var bytes = new byte[5];
		ByteBuffer.intLeHandler.set(bytes, 0, id);
		bytes[4] = (byte)userKey;
		return new Binary(bytes);
	}

	private static byte[] prefixOf(int id) {
		var bytes = new byte[4];
		ByteBuffer.intLeHandler.set(bytes, 0, id);
		return bytes;
	}

	private static List<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(RAFT).getNodes().values()) {
			var nodeConfig = RAFT.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
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

	private static final AtomicInteger tid = new AtomicInteger();

	private static void put(Dbh2Agent agent, String db, String table, List<Binary> keys) throws Exception {
		var batch = new BPrepareBatch.Data("", db, table, null);
		for (var k : keys)
			batch.getBatch().getPuts().put(k, new Binary(new byte[]{(byte)1}));
		batch.getBatch().setTid(tid.incrementAndGet());
		agent.prepareBatch(batch).await();
		agent.commitBatch(batch.getBatch().getTid()).await();
	}

	// 直连agent预填私有agents表，绕开proxy接线（形态对齐TestDbh2MultiBucketWalk）。
	@SuppressWarnings("unchecked")
	private static void registerDirectAgent(Dbh2AgentManager manager, String raft, Dbh2Agent agent) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("agents");
		field.setAccessible(true);
		var agents = (ConcurrentHashMap<String, Dbh2Agent>)field.get(manager);
		agents.put(raft, agent);
	}

	@Test
	public void testPrefixWalkDeliversTargetPrefix(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";

		var tableData = new MasterTable.Data();
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName(db);
		meta.setTableName(table);
		meta.setRaftConfig(RAFT);
		meta.setKeyFirst(Binary.Empty);
		meta.setKeyLast(Binary.Empty);
		tableData.getBuckets().put(Binary.Empty, meta);

		var rocks = new RocksDatabase(tempDir.resolve("dbh2PrefixWalk").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		Dbh2AgentManager manager = null;
		try {
			// stub master：openDatabase/createTableAsync不产生任何master网络调用。
			var stubMaster = new MasterAgent(new Config()) {
				@Override
				public void createTableAsync(String database, String tableName,
											 Action3<Integer, Boolean, MasterTable.Data> callback) {
					try {
						callback.run(0, true, tableData);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			manager = new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(),
					Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(tempDir).toString()), 830) {
				@Override
				public MasterAgent openDatabase(String masterName, String databaseName) {
					return stubMaster;
				}
			};

			setBucketMeta(agent, db, table, Binary.Empty, Binary.Empty);
			put(agent, db, table, List.of(pkey(1, 0xA), pkey(1, 0xB)));
			put(agent, db, table, List.of(pkey(2, 0xA), pkey(2, 0xB), pkey(2, 0xC)));
			put(agent, db, table, List.of(pkey(3, 0xA)));

			manager.putBuckets(tableData, "master830", db, table);
			registerDirectAgent(manager, RAFT, agent);

			// 全表asc+prefix=2：bug时seekToFirst落在prefix(1)的key上立即判定桶尾，0条（红）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "master830", db, table,
						(k, v) -> keys.add(new Binary(k)), false, prefixOf(2));
				Assertions.assertEquals(3, count, "asc全表walk必须收齐目标前缀的3条记录");
				Assertions.assertEquals(List.of(pkey(2, 0xA), pkey(2, 0xB), pkey(2, 0xC)), keys);
			}
			// 全表desc+prefix=2：bug时seekToLast落在prefix(3)的key上立即判定桶尾，0条（红）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "master830", db, table,
						(k, v) -> keys.add(new Binary(k)), true, prefixOf(2));
				Assertions.assertEquals(3, count, "desc全表walk必须收齐目标前缀的3条记录");
				Assertions.assertEquals(List.of(pkey(2, 0xC), pkey(2, 0xB), pkey(2, 0xA)), keys);
			}
			// 无prefix回归保护：全表6条两个方向（当前绿）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "master830", db, table,
						(k, v) -> keys.add(new Binary(k)), false, null);
				Assertions.assertEquals(6, count);
				Assertions.assertEquals(List.of(
						pkey(1, 0xA), pkey(1, 0xB), pkey(2, 0xA), pkey(2, 0xB), pkey(2, 0xC), pkey(3, 0xA)), keys);
				var descKeys = new ArrayList<Binary>();
				manager.walk(null, "master830", db, table,
						(k, v) -> descKeys.add(new Binary(k)), true, null);
				Assertions.assertEquals(List.of(
						pkey(3, 0xA), pkey(2, 0xC), pkey(2, 0xB), pkey(2, 0xA), pkey(1, 0xB), pkey(1, 0xA)), descKeys);
			}

			// prefix表层（Database.Dbh2PrefixTable）：desc分页空起点。
			// bug时首游标=裸4字节prefix（addPrefix(null)），服务端seekForPrev落在前缀区间下方，
			// 恒返回null游标→walkDesc整体报空表（红）。
			var databaseConf = new Config.DatabaseConf();
			databaseConf.setDatabaseType(Config.DbType.Dbh2);
			databaseConf.setDatabaseUrl("dbh2://127.0.0.1:11000/dbh2PrefixWalk");
			databaseConf.setName("dbh2");
			var database = new Database(null, manager, databaseConf);
			var prefixTable = (Zeze.Transaction.Database.AbstractKVTable)database.openTable("x___table1", 2);
			{
				var keys = new ArrayList<Binary>();
				ByteBuffer cursor = null;
				int rounds = 0;
				do {
					cursor = prefixTable.walkDesc(cursor, 2, (k, v) -> keys.add(new Binary(k)));
				} while (cursor != null && ++rounds < 100);
				Assertions.assertEquals(List.of(pkey(2, 0xC), pkey(2, 0xB), pkey(2, 0xA)), keys,
						"desc分页空起点必须收齐目标前缀记录");
			}
			// walkKeyDesc分页空起点（同款改动）。
			{
				var keys = new ArrayList<Binary>();
				ByteBuffer cursor = null;
				int rounds = 0;
				do {
					cursor = prefixTable.walkKeyDesc(cursor, 3, k -> keys.add(new Binary(k)));
				} while (cursor != null && ++rounds < 100);
				Assertions.assertEquals(List.of(pkey(2, 0xC), pkey(2, 0xB), pkey(2, 0xA)), keys);
			}
		} finally {
			if (null != manager)
				manager.stop();
			for (var dbh2 : nodes) {
				dbh2.close();
				LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
			}
			agent.close();
			rocks.close();
		}
	}
}
