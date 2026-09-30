package Zeze.Raft;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquiredState;
import Zeze.Config;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
@Timeout(10)
public class TestRaftExpectedTermCommit {
	@TempDir
	Path directory;

	@Test
	public void testTermChangeAfterBodyRollsBackBeforeAppend() throws Exception {
		Task.tryInitThreadPool();
		var config = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:17690">
					<node Host="127.0.0.1" Port="17690"/>
					<node Host="127.0.0.1" Port="17691"/>
					<node Host="127.0.0.1" Port="17692"/>
				</raft>
				""");
		config.setDbHome(directory.resolve("raft").toString());
		var rocks = new Rocks(null, RocksMode.Pessimism, config, new Config(), false);
		try {
			rocks.registerTableTemplate("ExpectedTerm", Integer.class, BAcquiredState.class);
			var table = rocks.<Integer, BAcquiredState>getTableTemplate("ExpectedTerm").openTable();
			var raft = rocks.getRaft();
			var sequence = raft.getLogSequence();
			var commitActions = new AtomicInteger();
			var rollbackActions = new AtomicInteger();
			raft.lock();
			try {
				// 不start server、不占端口：只模拟“旧任期操作到提交时已在新任期当选”。
				// 持Raft锁避免后台timer更改合成的角色；提交锁为可重入锁。
				var state = Raft.class.getDeclaredField("state");
				state.setAccessible(true);
				state.set(raft, Raft.RaftState.Leader);
				var expectedTerm = sequence.getTerm();
				var previousIndex = sequence.getLastIndex();
				var procedure = rocks.newProcedure(() -> {
					Transaction.getCurrent().runWhileCommit(commitActions::incrementAndGet);
					Transaction.getCurrent().runWhileRollback(rollbackActions::incrementAndGet);
					table.put(1, new BAcquiredState(1));
					sequence.trySetTerm(expectedTerm + 1);
					return 0;
				});
				assertEquals(-1, procedure.getExpectedTerm());
				procedure.setExpectedTerm(expectedTerm);
				assertEquals(Zeze.Transaction.Procedure.RaftRetry, procedure.call());
				assertEquals(previousIndex, sequence.getLastIndex(), "陈旧操作不能写入新任期日志");
				assertEquals(0, commitActions.get());
				assertEquals(1, rollbackActions.get());
				assertEquals(-1, rocks.newProcedure(() -> {
					assertNull(table.get(1), "被拒绝的事务不能发布内存记录");
					return -1;
				}).call());
			} finally {
				raft.unlock();
			}
		} finally {
			rocks.close();
		}
	}
}
