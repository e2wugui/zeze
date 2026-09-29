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
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 前缀降序遍历的排他上界回归：桶内恰存在严格等于 prefix 上界（prefixUpperBound(prefix)，
 * 末非0xFF字节+1截断）的key时，服务端 seekForPrev(上界) 的闭语义（<=target）会落在该边界key上，
 * 首个循环前缀过滤失败即误判桶尾，本桶目标前缀的记录被静默跳过（0条+桶尾）。
 * Walk/WalkKey 是公开 rpc，任意客户端可用普通用户前缀触发；框架内建前缀（4字节表id）因
 * 不写空用户key而不可达，属协议契约层缺陷（对齐 TestGAC02PrefixWalkPositioning 的补充边界）。
 * 拓扑：单桶[Empty,Empty)端口19250-19252，数据=prefix(1)×1、prefix(2)×2、边界key×1、prefix(3)×1。
 */
@Fast
public class TestPrefixWalkDescEqualUpperBoundKey {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19250"/>
				<node Host="127.0.0.1" Port="19251"/>
				<node Host="127.0.0.1" Port="19252"/>
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

	// prefix(2)=[02,00,00,00]的上界：末非0xFF字节(第4字节0x00)+1截断=[02,00,00,01]。
	// 它大于一切prefix(2)开头的key、小于一切prefix(3)开头的key；作为裸key写入桶即命中边界形态。
	private static final Binary upperBoundOfPrefix2 = new Binary(new byte[] {0x02, 0x00, 0x00, 0x01});

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
			batch.getBatch().getPuts().put(k, new Binary(new byte[] {(byte)1}));
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
	public void testPrefixWalkDescSkipsEqualUpperBoundKey(@TempDir Path tempDir) throws Exception {
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

		var rocks = new RocksDatabase(tempDir.resolve("dbh2PrefixUpperBound").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		Dbh2AgentManager manager = null;
		try {
			manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
					Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString()), 831);

			setBucketMeta(agent, db, table, Binary.Empty, Binary.Empty);
			put(agent, db, table, List.of(
					pkey(1, 0x0A), pkey(2, 0x0A), pkey(2, 0x0B), upperBoundOfPrefix2, pkey(3, 0x0A)));

			manager.putBuckets(tableData, "master831", db, table);
			registerDirectAgent(manager, RAFT, agent);

			// desc+prefix(2)：bug时seekForPrev(上界)恰好落在等于上界的key上，首循环前缀过滤
			// 失败即判桶尾，目标前缀2条记录被静默跳过（0条）（红）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "master831", db, table,
						(k, v) -> keys.add(new Binary(k)), true, prefixOf(2));
				Assertions.assertEquals(2, count, "desc前缀walk必须交付目标前缀的2条记录（不得被边界key误判桶尾）");
				Assertions.assertEquals(List.of(pkey(2, 0x0B), pkey(2, 0x0A)), keys);
			}
			// walkKey desc同款服务端路径（同一walkDesc实现）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walkKey(null, "master831", db, table,
						k -> keys.add(new Binary(k)), true, prefixOf(2));
				Assertions.assertEquals(2, count, "desc前缀walkKey必须交付目标前缀的2条记录");
				Assertions.assertEquals(List.of(pkey(2, 0x0B), pkey(2, 0x0A)), keys);
			}
			// asc+prefix(2)不受影响（seek(prefix)落在>=prefix的首key，等prefix的key匹配前缀）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "master831", db, table,
						(k, v) -> keys.add(new Binary(k)), false, prefixOf(2));
				Assertions.assertEquals(2, count);
				Assertions.assertEquals(List.of(pkey(2, 0x0A), pkey(2, 0x0B)), keys);
			}
			// 无prefix降序全桶回归保护：边界key本身按字节序正常交付（排序位在prefix(2)与prefix(3)之间）。
			{
				var keys = new ArrayList<Binary>();
				var count = manager.walk(null, "master831", db, table,
						(k, v) -> keys.add(new Binary(k)), true, null);
				Assertions.assertEquals(5, count);
				Assertions.assertEquals(List.of(
						pkey(3, 0x0A), upperBoundOfPrefix2, pkey(2, 0x0B), pkey(2, 0x0A), pkey(1, 0x0A)), keys);
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
