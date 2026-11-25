package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ProcessPrepareBatchRequest 必须在构造 Dbh2Transaction 加键锁之前完成 tid 去重、库表归属校验与迁出键过滤：
 * 1) 同 tid + 同 key 重试必须返回 eDuplicateTid（而非先撞键锁抛 lock timeout / Procedure.Exception），
 *    且不破坏存活事务及其键锁，后续 CommitBatch 正常落盘；
 * 2) 分桶后两笔并发陈旧 PrepareBatch 写同一已迁出键，均必须正常返回 refused 重定向而非因在旧桶锁住
 *    已迁出键互相触发 lock timeout。
 */
@Fast
public class TestPrepareValidationBeforeLocking {

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

	private static String buildRaftConfig(int[] ports) {
		return """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(ports[0], ports[1], ports[2]);
	}

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static Binary val(int i) {
		return new Binary(new byte[]{(byte)(0x20 + i)});
	}

	@Test
	void testSameTidSameKeyRetryReturnsDuplicateTidAndKeepsLiveTransaction(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(3));
		var database = new RocksDatabase(tempDir.resolve("dup_tid_same_key").toString());
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(raftConfig);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			var k1 = key(1);
			var v1 = val(1);

			// T1: prepare 成功并持锁存活于事务表。
			var first = new BPrepareBatch.Data("", "database", "table1", null);
			first.getBatch().setTid(1L);
			first.getBatch().getPuts().put(k1, v1);
			var f1 = agent.prepareBatch(first);
			f1.await();
			Assertions.assertEquals(0, f1.get().getResultCode());

			var leader = RaftBucketTopologySupport.waitLeader(nodes);
			Assertions.assertTrue(leader.getStateMachine().getTransactions().containsKey(1L));

			// 同 tid + 同 key 重试：必须直达 eDuplicateTid，不得先撞键锁返回 Procedure.Exception。
			var retry = new BPrepareBatch.Data("", "database", "table1", null);
			retry.getBatch().setTid(1L);
			retry.getBatch().getPuts().put(k1, v1);
			var fRetry = agent.prepareBatch(retry);
			fRetry.await();
			var rcRetry = fRetry.get().getResultCode();
			Assertions.assertTrue(rcRetry != 0 && rcRetry != Procedure.RaftApplied);
			Assertions.assertEquals(AbstractDbh2.eDuplicateTid, IModule.getErrorCode(rcRetry),
					"同 tid 同 key 重试必须返回 eDuplicateTid（而非 lock timeout）");

			// 存活事务与键锁均保持完好：其他 tid 写同键仍互斥。
			Assertions.assertTrue(leader.getStateMachine().getTransactions().containsKey(1L));
			var other = new BPrepareBatch.Data("", "database", "table1", null);
			other.getBatch().setTid(2L);
			other.getBatch().getPuts().put(k1, val(2));
			var fOther = agent.prepareBatch(other);
			fOther.await();
			Assertions.assertEquals(Procedure.Exception, fOther.get().getResultCode(),
					"存活事务 T1 仍应独占 k1 键锁");

			// T1 提交后数据正常落盘可读。
			Assertions.assertEquals(0, agent.commitBatch(1L).await().get().getResultCode());
			Assertions.assertEquals(v1, RaftBucketTopologySupport.get(agent, k1));
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}

	@Test
	void testConcurrentStalePreparesForMovedKeyBothRedirectWithoutLockTimeout(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(3));
		var targetRaft = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="29891"/>
				</raft>
				""";
		var database = new RocksDatabase(tempDir.resolve("concurrent_moved_key").toString());
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(raftConfig);
		try {
			var initialMeta = new BBucketMeta.Data();
			initialMeta.setDatabaseName("database");
			initialMeta.setTableName("table1");
			initialMeta.setRaftConfig(raftConfig);
			initialMeta.setKeyFirst(Binary.Empty);
			initialMeta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(initialMeta);

			// 模拟分桶收尾：源桶收窄为 [Empty, k5)，[k5, Empty) 迁往 targetRaft。
			var fromMeta = initialMeta.copy();
			fromMeta.setKeyLast(key(5));
			var toMeta = new BBucketMeta.Data();
			toMeta.setDatabaseName("database");
			toMeta.setTableName("table1");
			toMeta.setRaftConfig(targetRaft);
			toMeta.setKeyFirst(key(5));
			toMeta.setKeyLast(Binary.Empty);

			var leader = RaftBucketTopologySupport.waitLeader(nodes);
			leader.getRaft().appendLog(new LogEndSplit(fromMeta, toMeta));

			var localKey = key(1);
			var movedKey = key(7);

			// 事务 T1（混合本桶键 + 已迁出键）：先 prepare 并保持未提交存活。
			var b1 = new BPrepareBatch.Data("", "database", "table1", null);
			b1.getBatch().setTid(10L);
			b1.getBatch().getPuts().put(localKey, val(1));
			b1.getBatch().getPuts().put(movedKey, val(7));
			var f1 = agent.prepareBatch(b1);
			f1.await();
			Assertions.assertEquals(0, f1.get().getResultCode());
			Assertions.assertEquals(val(7),
					requireNonNull(f1.get().Result.getRefused().get(targetRaft)).getPuts().get(movedKey),
					"T1 的迁出键必须进入 refused 重定向");

			// 事务 T2 与 T3 并发写同一已迁出键 movedKey（T1 仍存活未提交）：
			// 旧桶未锁定 movedKey，T2/T3 均必须正常拿到 refused 重定向而非 lock timeout。
			var b2 = new BPrepareBatch.Data("", "database", "table1", null);
			b2.getBatch().setTid(11L);
			b2.getBatch().getPuts().put(movedKey, val(8));
			var f2 = agent.prepareBatch(b2);
			f2.await();
			Assertions.assertEquals(0, f2.get().getResultCode(),
					"并发陈旧 PrepareBatch 写已迁出键不得因旧桶锁住迁出键而报 lock timeout");
			Assertions.assertEquals(val(8),
					requireNonNull(f2.get().Result.getRefused().get(targetRaft)).getPuts().get(movedKey));

			var b3 = new BPrepareBatch.Data("", "database", "table1", null);
			b3.getBatch().setTid(12L);
			b3.getBatch().getDeletes().add(movedKey);
			var f3 = agent.prepareBatch(b3);
			f3.await();
			Assertions.assertEquals(0, f3.get().getResultCode(),
					"并发陈旧 PrepareBatch 删除已迁出键同样必须返回 refused 重定向");
			Assertions.assertTrue(
					requireNonNull(f3.get().Result.getRefused().get(targetRaft)).getDeletes().contains(movedKey));

			// T1 仍应锁定 localKey，并可正常提交。
			Assertions.assertEquals(0, agent.commitBatch(10L).await().get().getResultCode());
			Assertions.assertEquals(0, agent.commitBatch(11L).await().get().getResultCode());
			Assertions.assertEquals(0, agent.commitBatch(12L).await().get().getResultCode());
			Assertions.assertEquals(val(1), RaftBucketTopologySupport.get(agent, localKey));
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}
}
