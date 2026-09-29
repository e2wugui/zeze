package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 桶侧自主 undo 与协调者 commit 决策的围栏冲突终局（dbh2-03 断根）：undo 落日志时协调者
 * 决策未知——修复前事务立即毁尸（blob 删除、锁释放），迟到的 LogCommitBatch 落 not-found
 * warn、rc=0，客户端确认成功而数据灭失。修复后自主 undo 走未确认墓碑：迟到 commit 在
 * 墓碑窗内复活并提交（数据不丢、成功变真）；协调者 UndoBatch 到达=确认终局，物理删除；
 * 墓碑窗超时仍未决=物理删除并响亮告警。形态：进程内 raft 桶直驱 leader 状态机
 * （对齐 TestGAC02PrefixWalkPositioning 的 harness）。
 */
@Fast
public class TestUndoCommitFenceResurrect {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19310"/>
				<node Host="127.0.0.1" Port="19311"/>
				<node Host="127.0.0.1" Port="19312"/>
			</raft>
			""";

	private static List<Dbh2> startBucket(RocksDatabase database, Path tempDir) {
		var nodes = new ArrayList<Dbh2>();
		for (var config : RaftConfig.loadFromString(RAFT).getNodes().values()) {
			var nodeConfig = RAFT.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static Dbh2StateMachine leaderStateMachine(List<Dbh2> nodes) throws InterruptedException {
		for (var i = 0; i < 300; ++i) { // 选举收敛等待（对齐 harness 常规轮询形态）
			for (var n : nodes)
				if (n.getStateMachine().getRaft().isLeader())
					return n.getStateMachine();
			Thread.sleep(50);
		}
		throw new AssertionError("raft leader not elected");
	}

	private static void stopBucket(List<Dbh2> nodes) {
		for (var n : nodes)
			n.close();
	}

	/** 经 raft prepare 一笔单 key 事务，返回批次（tid 已设）。 */
	private static BPrepareBatch.Data prepare(Dbh2Agent agent, long tid, Binary key) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().put(key, new Binary(new byte[]{(byte)9}));
		batch.getBatch().setTid(tid);
		agent.prepareBatch(batch).await();
		return batch;
	}

	/** 围栏冲突：自主 undo 后迟到的 commit 必须复活并提交——数据不丢，成功变真。 */
	@Test
	public void testFenceConflictResurrectsCommit(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{1});
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FenceResurrect").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			prepare(agent, 100L, key);
			var leader = leaderStateMachine(nodes);
			waitTransactionApplied(leader, 100L); // raft 应用收敛：直接驱动前事务必在表

			leader.undoBatch(100L, false); // 桶侧自主超时 undo（onTimer 形态）
			leader.commitBatch(100L); // 协调者已持久化 eCommitting 后迟到的 LogCommitBatch

			Assertions.assertEquals(9, leader.getBucket().get(key).bytesUnsafe()[0],
					"围栏冲突必须以提交终局复活（修复前：undo 毁尸、commit 落 not-found warn，数据灭失）");
		} finally {
			stopBucket(nodes);
			rocks.close();
		}
	}

	/** 协调者 UndoBatch 追认自主 undo：终局确认后迟到的重复 commit 不得复活。 */
	@Test
	public void testCoordinatorUndoConfirmsTombstone(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{2});
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FenceConfirm").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			prepare(agent, 200L, key);
			var leader = leaderStateMachine(nodes);
			waitTransactionApplied(leader, 200L);
			leader.undoBatch(200L, false); // 自主 undo：未确认墓碑
			leader.undoBatch(200L, true); // 协调者 UndoBatch：确认 undo 终局，物理删除
			leader.commitBatch(200L); // 迟到的重复 commit：不得复活已确认的 undo

			Assertions.assertNull(leader.getBucket().get(key), "已确认 undo 后迟到 commit 不得复活");
		} finally {
			stopBucket(nodes);
			rocks.close();
		}
	}

	/** 墓碑窗超时仍未决：物理删除并告警，此后迟到 commit 不复活（协调者 rpc 必已失败、客户端已见失败）。 */
	@Test
	public void testTombstoneExpiryPhysicalUndo(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{3});
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FenceExpire").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			prepare(agent, 300L, key);
			var leader = leaderStateMachine(nodes);
			waitTransactionApplied(leader, 300L);
			leader.undoBatch(300L, false);
			leader.expireDueTombstones(0); // 确定性超窗清扫（生产为 onTimer 周期+配置窗）
			leader.commitBatch(300L);

			Assertions.assertNull(leader.getBucket().get(key), "超窗墓碑物理删除后迟到 commit 不得复活");
			leader.expireDueTombstones(0); // 幂等：空墓碑表再扫不炸
		} finally {
			stopBucket(nodes);
			rocks.close();
		}
	}

	/** 直接驱动状态机前等待 raft 应用收敛（事务入表即 prepare 已 apply）。 */
	private static void waitTransactionApplied(Dbh2StateMachine leader, long tid) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			if (leader.getTransactions().containsKey(tid))
				return;
			Thread.sleep(50);
		}
		throw new AssertionError("prepare not applied: tid=" + tid);
	}
}
