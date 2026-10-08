package Zeze.Dbh2;

import harness.Extra;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDBException;

/**
 * 桶侧自主 undo 与协调者 commit 决策的围栏冲突终局（dbh2-03 断根）：undo 落日志时协调者
 * 决策未知——修复前事务立即毁尸（blob 删除、锁释放），迟到的 LogCommitBatch 落 not-found
 * warn、rc=0，客户端确认成功而数据灭失。修复后自主 undo 走未确认墓碑：迟到 commit 在
 * 墓碑窗内复活并提交（数据不丢、成功变真）；协调者 UndoBatch 到达=确认终局，物理删除；
 * 墓碑窗超时仍未决=物理删除并响亮告警。形态：进程内 raft 桶直驱 leader 状态机
 * （对齐 TestPrefixWalkPositioning 的 harness）。
 */
@Fast
@Extra
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

	// 重启装载测试用：调小SnapshotLogCount让少量日志即跨快照边界（Dbh2的
	// SnapshotCommitDelayed下已提交快照=倒数第二代，见LogSequence.commitSnapshot）。
	private static final String RAFT_SNAPSHOT = RAFT.replaceFirst("<raft ",
			"<raft SnapshotLogCount=\"10\" ");

	private static List<Dbh2> startBucket(RocksDatabase database, Path tempDir) {
		return startBucket(database, tempDir, RAFT);
	}

	private static List<Dbh2> startBucket(RocksDatabase database, Path tempDir, String raftConfig) {
		var nodes = new ArrayList<Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfig).getNodes().values()) {
			var nodeConfig = raftConfig.replaceFirst("<raft ",
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
		for (var n : nodes) {
			n.close();
			Zeze.Raft.LogSequence.deleteDirectory(new java.io.File(n.getRaft().getRaftConfig().getDbHome()));
		}
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
			agent.close();
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
			agent.close();
			rocks.close();
		}
	}

	/** 墓碑窗超时仍未决：终局删除并告警，此后迟到 commit 不复活（协调者 rpc 必已失败、
	 * 客户端已见失败）。清扫经raft日志（dbh2-02）apply异步——直驱commit前先轮询等
	 * 日志apply收敛（直驱改轮询，df318ce58 同款判据适配）。 */
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
			waitExpiryApplied(leader, 300L); // 等终局日志apply（摘墓碑+物理删除）
			leader.commitBatch(300L);

			Assertions.assertNull(leader.getBucket().get(key), "超窗墓碑物理删除后迟到 commit 不得复活");
			leader.expireDueTombstones(0); // 幂等：空墓碑表再扫不炸
		} finally {
			stopBucket(nodes);
			agent.close();
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

	/** 墓碑化经 raft 日志后等待 apply 收敛（墓碑入表、事务出表、锁已释放）。 */
	private static void waitTombstoned(Dbh2StateMachine leader, long tid) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			if (leader.isTombstonedPending(tid) && !leader.getTransactions().containsKey(tid))
				return;
			Thread.sleep(50);
		}
		throw new AssertionError("tombstone not applied: tid=" + tid);
	}

	/** 超窗清扫的终局日志已apply：墓碑出表（apply路径摘墓碑+物理删blob与marker）。 */
	private static void waitExpiryApplied(Dbh2StateMachine sm, long tid) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			if (!sm.isTombstonedPending(tid))
				return;
			Thread.sleep(50);
		}
		throw new AssertionError("expiry undo not applied: tid=" + tid);
	}

	private static void waitBucketValue(Dbh2StateMachine sm, Binary key, int expected, String message)
			throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			try {
				var v = sm.getBucket().get(key);
				if (null != v && v.size() > 0 && v.bytesUnsafe()[0] == expected)
					return;
			} catch (RocksDBException e) {
				// 轮询期间 rocks 读失败按未就绪处理
			}
			Thread.sleep(50);
		}
		Assertions.fail(message);
	}

	/** 等待trans列族指定键被终局清除（多数派commit不等于全副本apply，须轮询等apply收敛）。 */
	private static void waitTransCleared(Dbh2StateMachine sm, byte[] key, String message) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			try {
				if (null == sm.getBucket().getTrans().get(key))
					return;
			} catch (RocksDBException e) {
				// 轮询期间 rocks 读失败按未就绪处理
			}
			Thread.sleep(50);
		}
		Assertions.fail(message);
	}

	private static void tryClose(Dbh2 node) {
		try {
			node.close();
		} catch (Exception e) {
			// 清理尽力而为，不掩盖测试断言
		}
	}

	private static String rootMessage(Throwable t) {
		var sb = new StringBuilder();
		for (var e = t; null != e; e = e.getCause())
			sb.append(e.getMessage()).append(" <- ");
		return sb.toString();
	}

	private static List<String> nodeHomeNames() {
		var names = new ArrayList<String>();
		for (var config : RaftConfig.loadFromString(RAFT).getNodes().values())
			names.add(config.getName().replace(':', '_'));
		return names;
	}

	/** 灌日志过快照边界：持续prepare唯键事务，直到全副本已提交快照index>=requireIndex
	 * （延时提交下已提交快照为倒数第二代，其index覆盖的日志必已含此前全部状态）。
	 * 返回前静置等在途快照任务（backup重写共享DbHome/backup目录）排空，避免与
	 * 重启装载的extract/restore竞争。 */
	private static void pushLogsOverSnapshotBoundary(Dbh2Agent agent, List<Dbh2> nodes,
													 long firstTid, long requireIndex) throws Exception {
		var deadline = System.currentTimeMillis() + 60_000;
		for (var tid = firstTid; System.currentTimeMillis() < deadline; ++tid) {
			if (allCommittedSnapshotIndexAtLeast(nodes, requireIndex)) {
				Thread.sleep(1500); // 无新日志即无新快照任务，静置等在途者完成
				return;
			}
			prepare(agent, tid, fillerKey(tid));
		}
		throw new AssertionError("snapshot boundary not reached in time. requireIndex=" + requireIndex);
	}

	private static boolean allCommittedSnapshotIndexAtLeast(List<Dbh2> nodes, long requireIndex) {
		for (var n : nodes) {
			var file = new java.io.File(n.getRaft().getLogSequence().getCommittedSnapshotFile());
			if (!file.isFile())
				return false;
			var name = file.getName(); // snapshot.dat.<index>
			var index = Long.parseLong(name.substring(name.lastIndexOf('.') + 1));
			if (index < requireIndex)
				return false;
		}
		return true;
	}

	private static Binary fillerKey(long tid) {
		return new Binary(new byte[]{(byte)0xC0, (byte)tid, (byte)(tid >>> 8)});
	}

	/** 手工注入的trans blob编码（与Dbh2Transaction.prepareBatch同款）。 */
	private static byte[] encodeBatch(long tid, Binary key, byte value) {
		var batch = new BBatch.Data();
		batch.setTid(tid);
		batch.getPuts().put(key, new Binary(new byte[]{value}));
		return ByteBuffer.encode(batch).Copy();
	}

	/** 墓碑blob与后续同键事务blob重叠+快照+重启：装载必须按marker分态重建，不得把
	 * 两个重叠blob都当存活事务持锁重建——修复前第二个blob构造即lock timeout，Raft
	 * 构造失败，节点确定性崩溃循环，唯一恢复=人工删桶目录。墓碑化走raft日志（生产
	 * onTimer同款路径），全副本marker落盘；重启后复活能力必须保留（迟到commit复活
	 * 落盘），终局blob与marker同批清除。 */
	@Test
	public void testTombstoneOverlapBlobsSurviveRestart(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{4});
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FenceRestart").toString());
		var nodes = startBucket(rocks, tempDir, RAFT_SNAPSHOT);
		try {
			try {
				var agent = new Dbh2Agent(RAFT_SNAPSHOT);
				try {
					var meta = new BBucketMeta.Data();
					meta.setDatabaseName("database");
					meta.setTableName("table1");
					meta.setRaftConfig("");
					meta.setKeyFirst(Binary.Empty);
					meta.setKeyLast(Binary.Empty);
					agent.setBucketMeta(meta);

					prepare(agent, 400L, key); // blob_A
					var leader = leaderStateMachine(nodes);
					waitTransactionApplied(leader, 400L);
					// 桶侧自主undo经raft日志（生产onTimer同款）：全副本墓碑化，锁释放、blob保留
					leader.getRaft().appendLog(new LogUndoBatch(400L));
					waitTombstoned(leader, 400L);
					prepare(agent, 401L, key); // blob_B：同键重叠blob（锁已释放，prepare成功）

					// 灌日志过快照边界：blob_B为第4条日志，全副本已提交快照index>=4即含两个blob
					pushLogsOverSnapshotBoundary(agent, nodes, 402L, 4);
				} finally {
					agent.close();
				}
			} finally {
				for (var n : nodes)
					tryClose(n); // 关全部节点，保留DbHome
			}

			var rebooted = new ArrayList<Dbh2>();
			try {
				Exception startFail = null;
				try {
					rebooted.addAll(startBucket(rocks, tempDir, RAFT_SNAPSHOT));
				} catch (Exception ex) {
					startFail = ex;
				}
				Assertions.assertTrue(null == startFail,
						"重叠blob重启装载不得失败: " + rootMessage(startFail));

				var leader2 = leaderStateMachine(rebooted);
				Assertions.assertTrue(leader2.isTombstonedPending(400L),
						"墓碑必须经marker跨重启保留（复活能力不灭失）");
				Assertions.assertTrue(leader2.getTransactions().containsKey(401L),
						"墓碑后同键新事务必须存活重建");

				var agent2 = new Dbh2Agent(RAFT_SNAPSHOT);
				try {
					agent2.commitBatch(400L).await(); // 迟到commit复活
					waitBucketValue(leader2, key, 9, "墓碑事务的迟到commit必须复活落盘");
					agent2.commitBatch(401L).await(); // 后续事务终值
					for (var n : rebooted)
						waitBucketValue(n.getStateMachine(), key, 9, "全副本终值必须一致");
					// 终局后blob与marker同批清除（等全副本apply收敛）
					for (var n : rebooted) {
						var sm = n.getStateMachine();
						waitTransCleared(sm, Dbh2Transaction.transBlobKey(400L), "blob_A终局必须清除");
						waitTransCleared(sm, Dbh2Transaction.transTombstoneMarkerKey(400L), "marker_A终局必须清除");
						waitTransCleared(sm, Dbh2Transaction.transBlobKey(401L), "blob_B终局必须清除");
					}
				} finally {
					agent2.close();
				}
			} finally {
				for (var n : rebooted)
					tryClose(n);
			}
		} finally {
			for (var name : nodeHomeNames())
				Zeze.Raft.LogSequence.deleteDirectory(tempDir.resolve(name).toFile());
			rocks.close();
		}
	}

	/** 升级兼容：升级前已落盘的无marker重叠blob（旧版本墓碑态）装载不得失败——防御层
	 * 把锁竞争输者降级为墓碑（error留痕），启动成功且终局与分态重建收敛到同一结果
	 * （commit/undo对存活与墓碑的存储终局等价）。手工注入两个同键blob直写各副本trans列族。 */
	@Test
	public void testLegacyOverlapBlobsDegradeToTombstone(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{5});
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FenceLegacy").toString());
		var nodes = startBucket(rocks, tempDir, RAFT_SNAPSHOT);
		try {
			try {
				var agent = new Dbh2Agent(RAFT_SNAPSHOT);
				try {
					var meta = new BBucketMeta.Data();
					meta.setDatabaseName("database");
					meta.setTableName("table1");
					meta.setRaftConfig("");
					meta.setKeyFirst(Binary.Empty);
					meta.setKeyLast(Binary.Empty);
					agent.setBucketMeta(meta);

					// 手工注入两个无marker的重叠blob（升级前磁盘形态）：直写各副本trans列族
					for (var n : nodes) {
						var trans = n.getStateMachine().getBucket().getTrans();
						trans.put(Dbh2Transaction.transBlobKey(500L), encodeBatch(500L, key, (byte)5));
						trans.put(Dbh2Transaction.transBlobKey(501L), encodeBatch(501L, key, (byte)6));
					}
					// 灌日志过快照边界：注入先于一切快照，已提交快照必含注入blob
					pushLogsOverSnapshotBoundary(agent, nodes, 502L, 1);
				} finally {
					agent.close();
				}
			} finally {
				for (var n : nodes)
					tryClose(n); // 保留DbHome
			}

			var rebooted = new ArrayList<Dbh2>();
			try {
				Exception startFail = null;
				try {
					rebooted.addAll(startBucket(rocks, tempDir, RAFT_SNAPSHOT));
				} catch (Exception ex) {
					startFail = ex;
				}
				Assertions.assertTrue(null == startFail,
						"遗留重叠blob装载不得失败: " + rootMessage(startFail));

				var leader2 = leaderStateMachine(rebooted);
				// 防御层确定性：trans键序tid小者赢锁存活，输者降级为墓碑
				Assertions.assertTrue(leader2.getTransactions().containsKey(500L), "锁竞争赢者必须存活重建");
				Assertions.assertTrue(leader2.isTombstonedPending(501L), "输者必须降级为墓碑而非装载失败");

				var agent2 = new Dbh2Agent(RAFT_SNAPSHOT);
				try {
					agent2.commitBatch(500L).await(); // 存活路径提交
					waitBucketValue(leader2, key, 5, "存活事务提交必须落盘");
					agent2.commitBatch(501L).await(); // 墓碑复活路径提交
					for (var n : rebooted)
						waitBucketValue(n.getStateMachine(), key, 6, "全副本终值必须一致（后提交者胜）");
					for (var n : rebooted) {
						var sm = n.getStateMachine();
						waitTransCleared(sm, Dbh2Transaction.transBlobKey(500L), "blob_500终局必须清除");
						waitTransCleared(sm, Dbh2Transaction.transBlobKey(501L), "blob_501终局必须清除");
					}
				} finally {
					agent2.close();
				}
			} finally {
				for (var n : rebooted)
					tryClose(n);
			}
		} finally {
			for (var name : nodeHomeNames())
				Zeze.Raft.LogSequence.deleteDirectory(tempDir.resolve(name).toFile());
			rocks.close();
		}
	}

	/** 超窗清扫必须经raft日志：修复前leader本地直删trans blob（不经复制），follower的
	 * blob与内存墓碑原样保留——迟到的LogCommitBatch（经日志复制）在leader落not-found、
	 * 在follower落复活提交，同一日志跨副本apply分歧，三节点数据不一致（raft确定性破坏，
	 * leader快照安装可抹掉follower已复活的数据）。修复后清扫经终局undo日志：三副本墓碑
	 * 出表、blob+marker全网清除，迟到commit全网一致为空。 */
	@Test
	public void testTombstoneExpiryKeepsReplicasConsistent(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{6});
		var rocks = new RocksDatabase(tempDir.resolve("dbh2FenceExpiryConsistency").toString());
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

			prepare(agent, 600L, key);
			var leader = leaderStateMachine(nodes);
			waitTransactionApplied(leader, 600L);
			// 桶侧自主undo经raft日志（生产onTimer同款）：全副本墓碑化，锁释放、blob保留
			leader.getRaft().appendLog(new LogUndoBatch(600L));
			for (var n : nodes)
				waitTombstoned(n.getStateMachine(), 600L);

			leader.expireDueTombstones(0); // 确定性超窗清扫（生产为 onTimer 周期+配置窗）

			// 迟到CommitBatch经日志序落在expiry之后（生产形态：协调者解冻后deliver）
			agent.commitBatch(600L).await();

			// apply收敛标记：追加一笔可观测的prepare日志，全副本入表即已apply过commit
			prepare(agent, 601L, new Binary(new byte[]{7}));
			for (var n : nodes)
				waitTransactionApplied(n.getStateMachine(), 601L);

			// 三副本终态必须一致：清扫经日志后迟到commit全网一致为空（不得follower复活）
			for (var n : nodes) {
				var sm = n.getStateMachine();
				Assertions.assertNull(sm.getBucket().get(key),
						"超窗终局后迟到commit必须全网一致为空（不得跨副本分歧）");
				Assertions.assertFalse(sm.isTombstonedPending(600L),
						"清扫终局日志apply后墓碑必须出表");
				waitTransCleared(sm, Dbh2Transaction.transBlobKey(600L), "blob_600终局必须全网清除");
				waitTransCleared(sm, Dbh2Transaction.transTombstoneMarkerKey(600L), "marker_600终局必须全网清除");
			}
		} finally {
			stopBucket(nodes);
			agent.close();
			rocks.close();
		}
	}
}
