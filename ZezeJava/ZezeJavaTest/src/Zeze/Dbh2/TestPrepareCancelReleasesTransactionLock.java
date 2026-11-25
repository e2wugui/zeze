package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Net.Binary;
import Zeze.Raft.Raft;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PrepareBatch 在 Leader 追加 Raft 日志后、达成多数派提交前若被取消（换主/日志截断），
 * cancelCallback 必须从 stateMachine.transactions 移除预占事务并释放键锁，否则新主提交
 * 同键事务复制到降级后的 Follower 时，apply 撞上泄漏锁抛出 lock timeout 并触发 fatalKill。
 */
@Fast
public class TestPrepareCancelReleasesTransactionLock {

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
	void testCancelledPrepareReleasesTransactionAndKeyLock(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var ports = allocatePorts(3);
		var raftConfig = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(ports[0], ports[1], ports[2]);
		var database = new RocksDatabase(tempDir.resolve("prepare_cancel_lock").toString());
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

			var leaderA = RaftBucketTopologySupport.waitLeader(nodes);
			var fatalKilled = new AtomicBoolean(false);
			leaderA.getRaft().addAtFatalKill(() -> fatalKilled.set(true));

			// 关闭两个 Follower，使旧 Leader A 追加的 LogPrepareBatch(tid=1) 无法达成多数派提交。
			for (var node : nodes) {
				if (node != leaderA)
					node.close();
			}

			var key = new Binary(new byte[]{42});
			var val1 = new Binary(new byte[]{1, 1});
			var val2 = new Binary(new byte[]{2, 2});

			var cancelResultCode = new CompletableFuture<Long>();
			var prepare1 = new PrepareBatch() {
				@Override
				protected boolean sendResultCode(long code, @Nullable Binary result) {
					cancelResultCode.complete(code);
					return true;
				}
			};
			prepare1.Argument.setDatabase("database");
			prepare1.Argument.setTable("table1");
			prepare1.Argument.getBatch().setTid(1L);
			prepare1.Argument.getBatch().getPuts().put(key, val1);

			Assertions.assertEquals(0, leaderA.ProcessPrepareBatchRequest(prepare1));
			Assertions.assertTrue(leaderA.getStateMachine().getTransactions().containsKey(1L),
					"PrepareBatch 追加日志后应先占位内存事务表");
			Assertions.assertFalse(cancelResultCode.isDone(),
					"多数派不可达时 LogPrepareBatch 尚未提交或取消");

			// 模拟切主：Leader A 降级为 Follower，触发 cancelPendingAppendLogFutures()。
			leaderA.getRaft().lock();
			try {
				leaderA.getRaft().convertStateTo(Raft.RaftState.Follower);
			} finally {
				leaderA.getRaft().unlock();
			}

			Assertions.assertEquals(Procedure.CancelException, cancelResultCode.get(5, TimeUnit.SECONDS),
					"降级取消未提交日志应返回 CancelException");
			Assertions.assertFalse(leaderA.getStateMachine().getTransactions().containsKey(1L),
					"取消回调必须从事务表清理预占事务 tid=1");

			// 模拟新主提交同键事务 tid=2 并复制到已降级的 Follower A：apply 必须成功获取键锁而不触发 fatalKill。
			var batch2 = new BBatch.Data();
			batch2.setTid(2L);
			batch2.getPuts().put(key, val2);
			leaderA.getStateMachine().prepareBatch(batch2);
			leaderA.getStateMachine().commitBatch(2L);

			Assertions.assertFalse(fatalKilled.get(), "Follower A apply 新主同键事务不得触发 fatalKill");
			var isShutdownField = Zeze.Raft.Raft.class.getDeclaredField("isShutdown");
			isShutdownField.setAccessible(true);
			Assertions.assertFalse(isShutdownField.getBoolean(leaderA.getRaft()), "Follower A 不得因锁泄漏被杀死");
			Assertions.assertEquals(val2, requireNonNull(leaderA.getStateMachine().getBucket()).get(key),
					"Follower A 必须成功落盘新主事务数据");
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}
}
