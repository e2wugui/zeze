package Zeze.Dbh2;

import harness.Extra;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
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
 * 前缀降序遍历的越上界游标归一回归：exclusiveStartKey 高于前缀上界
 * （prefixUpperBound(prefix)，末非0xFF字节+1截断）时，seekForPrev(游标)落在
 * 区间上方的非前缀键上，首个循环的前缀过滤立即判桶尾，其下方的前缀键整段被
 * 静默跳过（0条+正常桶尾应答形态，缺行不可辨）。修复：游标高于上界时归一为
 * seekForPrev(prefixUpper) 定位（与空游标路径统一，复用等上界键跳过）。
 * prefix 表 API 允许任意用户游标入参（跨表陈旧游标/持久化游标过期复用），
 * 协议内部分页游标必匹配前缀不触发，属边界输入缺陷。
 * 拓扑：单桶[Empty,Empty)端口19270-19272，数据=prefix(2)×2、边界key×1、prefix(3)×1。
 */
@Fast
@Extra
public class TestPrefixWalkDescCursorAboveRange {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19270"/>
				<node Host="127.0.0.1" Port="19271"/>
				<node Host="127.0.0.1" Port="19272"/>
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

	// prefix(2)=[02,00,00,00]的上界=[02,00,00,01]：大于一切prefix(2)开头的key、
	// 小于一切prefix(3)开头的key；作为裸key写入桶即区间上方的非前缀键。
	private static final Binary prefix2Upper = new Binary(new byte[] {0x02, 0x00, 0x00, 0x01});

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

	private static final AtomicInteger tid = new AtomicInteger();

	private static void put(Dbh2Agent agent, String db, String table, List<Binary> keys) throws Exception {
		var batch = new BPrepareBatch.Data("", db, table, null);
		for (var k : keys)
			batch.getBatch().getPuts().put(k, new Binary(new byte[] {(byte)1}));
		batch.getBatch().setTid(tid.incrementAndGet());
		agent.prepareBatch(batch).await();
		agent.commitBatch(batch.getBatch().getTid()).await();
	}

	private static List<Binary> walkDesc(Dbh2Agent agent, Binary cursor, byte[] prefix) {
		var r = agent.walk(cursor, 10, true, prefix);
		Assertions.assertEquals(0, r.getResultCode());
		var keys = new ArrayList<Binary>();
		for (var kv : r.Result.getKeyValues())
			keys.add(kv.getKey());
		return keys;
	}

	@Test
	public void testCursorAbovePrefixUpperBoundDeliversPrefixRows(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";
		var rocks = new RocksDatabase(tempDir.resolve("dbh2WalkDescAboveRange").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName(db);
			meta.setTableName(table);
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			// 桶内：prefix(2)×2 + 边界key（恰等于prefixUpper，非前缀）+ prefix(3)×1（更上方的非前缀键）
			put(agent, db, table, List.of(pkey(2, 0x0A), pkey(2, 0x0B), prefix2Upper, pkey(3, 0x0C)));

			// 游标1=上界+尾巴（seekForPrev落在边界key上）：修复前首循环判桶尾0条（缺行）；
			// 修复后归一为上界定位（含等上界键跳过）交付prefix(2)全部2条。
			Assertions.assertEquals(List.of(pkey(2, 0x0B), pkey(2, 0x0A)),
					walkDesc(agent, new Binary(new byte[] {0x02, 0x00, 0x00, 0x01, 0x78}), prefixOf(2)),
					"越上界游标必须归一为前缀上界定位（不得静默跳过本桶前缀记录）");

			// 游标2=高于桶内全部键（seekForPrev落在prefix(3)键上）：同款归一，交付prefix(2)全部2条。
			Assertions.assertEquals(List.of(pkey(2, 0x0B), pkey(2, 0x0A)),
					walkDesc(agent, pkey(4, 0x00), prefixOf(2)),
					"高于全部键的游标必须从本桶前缀尾部开始交付");

			// 区间内游标不受影响（<上界走原路径）：排他交付1条。
			Assertions.assertEquals(List.of(pkey(2, 0x0A)),
					walkDesc(agent, pkey(2, 0x0B), prefixOf(2)),
					"区间内游标的排他降序语义不变");

			// 空游标：桶尾定位（既有等上界跳过路径）交付prefix(2)全部2条。
			Assertions.assertEquals(List.of(pkey(2, 0x0B), pkey(2, 0x0A)),
					walkDesc(agent, Binary.Empty, prefixOf(2)),
					"空游标桶尾定位交付全前缀记录");

			// 升序不受影响：越上界游标之后区间内无前缀键，正常0条结束（方向语义本就正确）。
			var asc = agent.walk(new Binary(new byte[] {0x02, 0x00, 0x00, 0x01, 0x78}), 10, false, prefixOf(2));
			Assertions.assertEquals(0, asc.getResultCode());
			Assertions.assertEquals(0, asc.Result.getKeyValues().size(), "升序越上界游标正常0条（升序方向不动）");
		} finally {
			for (var dbh2 : nodes) {
				dbh2.close();
				LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
			}
			agent.close();
			rocks.close();
		}
	}
}
