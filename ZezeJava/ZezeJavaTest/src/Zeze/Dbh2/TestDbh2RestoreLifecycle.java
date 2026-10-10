package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.Log;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Extra;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
@Extra
public class TestDbh2RestoreLifecycle {
	private static RaftConfig config(Path home) {
		return RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19480" DbHome="%s">
				    <node Host="127.0.0.1" Port="19480"/>
				    <node Host="127.0.0.1" Port="19481"/>
				    <node Host="127.0.0.1" Port="19482"/>
				</raft>
				""".formatted(home));
	}

	private static void tombstone(Dbh2StateMachine sm, long tid) {
		var batch = new BBatch.Data();
		batch.setTid(tid); // empty batch: no application or key lock is needed
		sm.prepareBatch(batch);
		sm.undoBatch(tid, false);
	}

	private static Dbh2 start(Path home) {
		var conf = config(home);
		return new Dbh2(null, conf.getName(), null, conf, new Config(), true, new TaskOneByOneByKey());
	}

	private static void apply(Dbh2 dbh2, long index, Log log) throws Exception {
		var sequence = dbh2.getRaft().getLogSequence();
		var holder = new RaftLog(1, index, log);
		var save = LogSequence.class.getDeclaredMethod("saveLog", RaftLog.class);
		var apply = LogSequence.class.getDeclaredMethod("tryApply", RaftLog.class, long.class);
		save.setAccessible(true);
		apply.setAccessible(true);
		dbh2.getRaft().lock();
		try {
			save.invoke(sequence, holder);
			apply.invoke(sequence, holder, 1L);
		} finally {
			dbh2.getRaft().unlock();
		}
	}

	private static BBatch.Data batch(long tid, Binary key) {
		var batch = new BBatch.Data();
		batch.setTid(tid);
		batch.getPuts().put(key, new Binary(new byte[]{42}));
		return batch;
	}

	private static LogPrepareBatch prepare(BBatch.Data batch) {
		var request = new PrepareBatch();
		request.Argument.setBatch(batch);
		return new LogPrepareBatch(request);
	}

	private static LogCommitBatch commit(long tid) {
		var request = new CommitBatch();
		request.Argument.setTid(tid);
		return new LogCommitBatch(request);
	}

	@Test
	public void watermarkRestartReacquiresLiveKeyLocks(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{1});
		try (var dbh2 = start(home)) {
			apply(dbh2, 1, prepare(batch(101, key)));
		}
		try (var dbh2 = start(home)) {
			var sm = dbh2.getStateMachine();
			assertEquals(1, dbh2.getRaft().getLogSequence().getLastApplied());
			assertTrue(sm.getTransactions().containsKey(101L));
			assertThrows(RuntimeException.class, () -> new Dbh2Transaction(dbh2, batch(202, key)),
					"restored live transaction must exclude another transaction on the same key");
			apply(dbh2, 2, new LogUndoBatch(101, false));
			try (var next = new Dbh2Transaction(dbh2, batch(202, key))) {
				assertEquals(202, next.getBatch().getTid(), "tombstoning releases the recovered lock");
			}
			apply(dbh2, 3, commit(101));
			assertEquals(new Binary(new byte[]{42}), sm.getBucket().get(key));
			assertFalse(sm.isTombstonedPending(101));
		}
	}

	@Test
	public void snapshotReplacementRejectsOldQueueAck(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var key = new Binary(new byte[]{1});
		try (var dbh2 = start(home)) {
			var sm = dbh2.getStateMachine();
			var splitting = new BBucketMeta.Data();
			splitting.setKeyFirst(key);
			apply(dbh2, 1, new LogSetSplittingMeta(splitting));
			apply(dbh2, 2, prepare(batch(101, key)));
			apply(dbh2, 3, commit(101));
			var oldBatch = sm.pollSplitSync(10);
			assertNotNull(oldBatch);
			assertEquals(new Binary(new byte[]{42}), oldBatch.data.getPuts().get(key));
			dbh2.getRaft().getRaftConfig().setSnapshotCommitDelayed(false);
			assertTrue(sm.snapshot(home.resolve("queue.zip").toString()).success);
			sm.loadSnapshot(dbh2.getRaft().getLogSequence().getCommittedSnapshotFile());
			sm.advanceSplitSyncWatermark(oldBatch);
			assertTrue(sm.hasPendingSplitSync(), "ACK from the replaced database must not drain restored obligations");
			var currentBatch = sm.pollSplitSync(10);
			assertNotNull(currentBatch);
			assertNotEquals(oldBatch.generation, currentBatch.generation);
			sm.advanceSplitSyncWatermark(currentBatch);
			assertFalse(sm.hasPendingSplitSync());
		}
	}

	@Test
	public void restoreRebuildsOnlySnapshotTombstones(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var sm = new Dbh2StateMachine(null);
		var raft = new Raft(sm, null, config(home), new Config());
		try {
			sm.openBucket();
			tombstone(sm, 1);
			var snapshot = home.resolve("base.zip").toString();
			assertTrue(sm.snapshot(snapshot).success);
			tombstone(sm, 2);
			assertTrue(sm.isTombstonedPending(2));
			sm.loadSnapshot(raft.getLogSequence().getCommittedSnapshotFile());
			assertTrue(sm.isTombstonedPending(1));
			assertFalse(sm.isTombstonedPending(2), "old memory tombstone must not survive snapshot replacement");
			sm.commitBatch(2);
			var tidKey = ByteBuffer.Allocate();
			tidKey.WriteLong(2);
			assertNull(sm.getBucket().getTrans().get(tidKey.Copy()));
		} finally {
			raft.shutdown();
			sm.close();
		}
	}

	@Test
	public void restorePreservesSynchronousWrites(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var sm = new Dbh2StateMachine(null);
		var raft = new Raft(sm, null, config(home), new Config());
		try {
			sm.openBucket();
			sm.getBucket().setWriteOptions(RocksDatabase.getSyncWriteOptions());
			assertTrue(sm.snapshot(home.resolve("base.zip").toString()).success);
			sm.loadSnapshot(raft.getLogSequence().getCommittedSnapshotFile());
			assertTrue(sm.getBucket().getWriteOptions().sync(), "snapshot restore must preserve sync writes");
		} finally {
			raft.shutdown();
			sm.close();
		}
	}

	@Test
	public void dbh2EnablesDelayedSnapshotsOnRuntimeConfig(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var conf = config(home);
		try (var dbh2 = new Dbh2(null, conf.getName(), null, conf, new Config(), true,
				new TaskOneByOneByKey())) {
			assertTrue(dbh2.getRaft().getRaftConfig().isSnapshotCommitDelayed(),
					"Dbh2 must set the derived runtime configuration");
			assertFalse(conf.isSnapshotCommitDelayed(), "caller configuration stays unchanged");
		}
	}
}
