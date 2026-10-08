package Zeze.Dbh2;

import harness.Extra;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 收尾写序回归（endSplit/endMove）：查询协议锁外并发派发（架构明示），收尾apply内
 * "meta切换与数据删除"两段写的顺序决定窗口内并发Get的应答——原序（先deleteToEnd后
 * setBucketMeta）窗口内Get以旧meta通过inBucket、读已删数据，把已迁往新桶、仍存在的
 * 键应答为权威"不存在"（静默假缺失，客户端不重试则错误结论被采纳）；修复后（meta先
 * 收窄/置死、deleteToEnd后行）窗口内Get要么命中值、要么eBucketMismatch→KV(false)
 * 重路由，无权威null。get风暴多轮统计断言+功能等价（meta已窄/死、迁出键已删、
 * 未迁出键保留）。
 * 拓扑：单桶[Empty,Empty)端口19330-19332。
 */
@Fast
@Extra
public class TestGetDuringFinalizeNoFalseMiss {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19330"/>
				<node Host="127.0.0.1" Port="19331"/>
				<node Host="127.0.0.1" Port="19332"/>
			</raft>
			""";

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

	private static Dbh2StateMachine waitLeader(List<Zeze.Dbh2.Dbh2> nodes) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			for (var n : nodes)
				if (n.getRaft().isLeader())
					return n.getStateMachine();
			Thread.sleep(50);
		}
		throw new AssertionError("raft leader not elected");
	}

	private static final AtomicInteger tid = new AtomicInteger();

	private static void put(Dbh2Agent agent, String db, String table, Binary key) throws Exception {
		var batch = new BPrepareBatch.Data("", db, table, null);
		batch.getBatch().getPuts().put(key, new Binary(new byte[] {(byte)9}));
		batch.getBatch().setTid(tid.incrementAndGet());
		agent.prepareBatch(batch).await();
		agent.commitBatch(batch.getBatch().getTid()).await();
	}

	// get风暴：持续get目标键，统计"权威null"（KV(true,null)——键仍存在（已迁新桶）
	// 却被应答不存在）出现次数。命中值与KV(false)（eBucketMismatch重路由）都合法。
	private static final class GetStorm extends Thread {
		private final Dbh2Agent agent;
		private final String db;
		private final String table;
		private final Binary key;
		private final AtomicBoolean stop = new AtomicBoolean();
		final AtomicLong authoritativeNull = new AtomicLong();
		final AtomicLong total = new AtomicLong();

		GetStorm(Dbh2Agent agent, String db, String table, Binary key) {
			this.agent = agent;
			this.db = db;
			this.table = table;
			this.key = key;
		}

		@Override
		public void run() {
			while (!stop.get()) {
				total.incrementAndGet();
				var kv = agent.get(db, table, key);
				if (kv.getKey() && null == kv.getValue())
					authoritativeNull.incrementAndGet();
			}
		}
	}

	/** 等风暴累计到目标轮次（计数驱动，负载无关）：观察窗从"固定时间窗"改为"固定轮次窗"
	 * ——原 sleep(300)+sleep(100) 时间窗内每轮是一次完整raft RPC(~8-10ms)，正常负载恰好
	 * 压线50、负载一抖即44-48，">=50统计有效"断言在墙钟预算下假红（生涯×5：44/15/45/48，
	 * 2026-10-08批r4实证）。就位/观察都等计数达标（有界），finalize持锁期间风暴阻塞也
	 * 在预算内被吸收；超时=风暴无进展（真故障，不掩盖）。 */
	private static void awaitStormCount(GetStorm storm, long target, long timeoutMs) throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (storm.total.get() < target) {
			if (System.currentTimeMillis() >= deadline)
				throw new IllegalStateException("storm made no progress: total=" + storm.total.get() + " target=" + target);
			Thread.sleep(10);
		}
	}

	@Test
	public void testGetStormDuringEndSplitSeesValueOrRedirect(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FinalizeSplit").toString());
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
			var leader = waitLeader(nodes);

			var survivor = new Binary(new byte[]{0x01}); // 恒低于一切分界，永不迁出
			put(agent, db, table, survivor);

			// 三轮收窄：round i 迁出键 k_i（≥分界 M_i），分界严格递减、迁出键始终在
			// 当前桶内（prepare可通过），每轮get风暴跨骑收尾窗口。
			var boundaries = new Binary[]{new Binary(new byte[]{0x28}), new Binary(new byte[]{0x18}), new Binary(new byte[]{0x08})};
			var movedKeys = new Binary[]{new Binary(new byte[]{0x30}), new Binary(new byte[]{0x20}), new Binary(new byte[]{0x10})};
			for (var round = 0; round < boundaries.length; ++round) {
				var key = movedKeys[round];
				var boundary = boundaries[round];
				put(agent, db, table, key);
				var before = agent.get(db, table, key).getValue();
				Assertions.assertEquals(9, before.Bytes[before.ReadIndex], "迁出键收尾前可读");

				var storm = new GetStorm(agent, db, table, key);
				storm.start();
				awaitStormCount(storm, 10, 10_000); // 风暴就位（计数驱动替代sleep(300)）
				var from = leader.getBucket().getBucketMeta().copy();
				from.setKeyLast(boundary);
				var to = leader.getBucket().getBucketMeta().copy();
				to.setKeyFirst(boundary);
				to.setRaftConfig(RAFT);
				var base = storm.total.get();
				leader.endSplit(from, to); // 收尾（原缺陷：µs级"数据已删、meta未切"窗口）
				awaitStormCount(storm, base + 50, 10_000); // 观察窗=收尾后50轮（计数驱动替代sleep(100)）
				storm.stop.set(true);
				storm.join();

				Assertions.assertEquals(0, storm.authoritativeNull.get(),
						"endSplit窗口内Get只应命中值或KV(false)重路由，不得权威null（静默假缺失）");
				// 功能等价：meta已收窄、迁出键已物理删除、未迁出键保留。
				Assertions.assertEquals(0, boundary.compareTo(leader.getBucket().getBucketMeta().getKeyLast()),
						"endSplit后meta必须收窄到分界");
				Assertions.assertNull(leader.getBucket().getData().get(key.bytesUnsafe(), key.getOffset(), key.size()),
						"迁出键必须已被物理删除");
			}
			var survivorValue = agent.get(db, table, survivor).getValue();
			Assertions.assertEquals(9, survivorValue.Bytes[survivorValue.ReadIndex], "未迁出键收尾后必须仍可读");
		} finally {
			for (var n : nodes) {
				n.close();
				LogSequence.deleteDirectory(new File(n.getRaft().getRaftConfig().getDbHome()));
			}
			agent.close();
			rocks.close();
		}
	}

	@Test
	public void testGetStormDuringEndMoveSeesValueOrRedirect(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FinalizeMove").toString());
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
			var leader = waitLeader(nodes);

			var key = new Binary(new byte[]{0x55}); // endMove窗口覆盖全桶：任意键
			put(agent, db, table, key);
			var before = agent.get(db, table, key).getValue();
				Assertions.assertEquals(9, before.Bytes[before.ReadIndex], "迁出键收尾前可读");

			var storm = new GetStorm(agent, db, table, key);
			storm.start();
			awaitStormCount(storm, 10, 10_000); // 风暴就位（计数驱动替代sleep(300)）
			var to = leader.getBucket().getBucketMeta().copy();
			to.setRaftConfig(RAFT);
			var base = storm.total.get();
			leader.endMove(to); // 收尾置死全桶（原缺陷：全桶键域的假缺失窗口）
			awaitStormCount(storm, base + 50, 10_000); // 观察窗=收尾后50轮（计数驱动替代sleep(100)）
			storm.stop.set(true);
			storm.join();

			Assertions.assertEquals(0, storm.authoritativeNull.get(),
					"endMove窗口内Get只应命中值或KV(false)重路由，不得权威null（静默假缺失）");
			// 功能等价：meta已置死、数据已物理删除、后续get恒重路由。
			var deadMeta = leader.getBucket().getBucketMeta();
			Assertions.assertNotEquals(0, deadMeta.getKeyFirst().compareTo(Binary.Empty), "endMove后meta必须置死");
			Assertions.assertNull(leader.getBucket().getData().get(key.bytesUnsafe(), key.getOffset(), key.size()),
					"迁出键必须已被物理删除");
			Assertions.assertFalse(agent.get(db, table, key).getKey(), "死桶get必须KV(false)重路由");
		} finally {
			for (var n : nodes) {
				n.close();
				LogSequence.deleteDirectory(new File(n.getRaft().getRaftConfig().getDbHome()));
			}
			agent.close();
			rocks.close();
		}
	}
}
