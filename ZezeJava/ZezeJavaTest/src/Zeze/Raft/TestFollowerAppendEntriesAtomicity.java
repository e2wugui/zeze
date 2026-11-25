package Zeze.Raft;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestFollowerAppendEntriesAtomicity {
	private static Raft newRaft(int portBase, Path home) throws Exception {
		var name = "127.0.0.1:" + portBase;
		var conf = RaftConfig.loadFromString("""
				<raft Name="%s" DbHome="%s">
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(name, home, portBase, portBase + 1, portBase + 2));
		return new Raft(new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		}, null, conf, new Config());
	}

	private static void setLastIndex(LogSequence seq, long value) throws Exception {
		var f = LogSequence.class.getDeclaredField("lastIndex");
		f.setAccessible(true);
		f.setLong(seq, value);
	}

	private static void deleteRawLog(LogSequence seq, long index) throws Exception {
		var f = LogSequence.class.getDeclaredField("logs");
		f.setAccessible(true);
		var logs = (RocksDatabase.Table)f.get(seq);
		assertNotNull(logs);
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(index);
		logs.delete(seq.getWriteOptions(), key.Bytes, 0, key.WriteIndex);
	}

	@Test
	void followerRepairsHoleAndTruncatesStaleSuffixAtomically(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var raft = newRaft(19740, home);
		try {
			raft.lock();
			try {
				var seq = raft.getLogSequence();
				seq.trySetTerm(1);
				for (long idx = 1; idx <= 4; idx++)
					seq.saveLog(new RaftLog(1, idx, new HeartbeatLog()));
				setLastIndex(seq, 4);

				// 在 [firstIndex, lastIndex] 中制造空洞（删掉 index=2，保留 3、4，模拟截断中途崩溃残留）。
				deleteRawLog(seq, 2);
				assertNull(seq.readLog(2));

				var ae = new AppendEntries();
				ae.Argument.setTerm(2);
				ae.Argument.setLeaderId("127.0.0.1:19741");
				ae.Argument.setPrevLogIndex(1);
				ae.Argument.setPrevLogTerm(1);
				ae.Argument.setLeaderCommit(0);
				ae.Argument.getEntries().add(new Binary(new RaftLog(2, 2, new HeartbeatLog()).encode()));
				ae.Argument.getEntries().add(new Binary(new RaftLog(2, 3, new HeartbeatLog()).encode()));
				ae.Argument.setLastEntryIndex(3);

				seq.followerOnAppendEntries(ae);
				assertTrue(ae.Result.getSuccess());
				assertEquals(3, seq.getLastIndex());
				var log2 = seq.readLog(2);
				var log3 = seq.readLog(3);
				assertNotNull(log2, "空洞处的日志必须被补齐，不得静默跳过");
				assertEquals(2, log2.getTerm());
				assertNotNull(log3);
				assertEquals(2, log3.getTerm());
				assertNull(seq.readLog(4), "旧分叉后缀必须被一并截断");
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void followerConflictTruncatesUniqueStubsAndCancelsCallbacks(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var raft = newRaft(19750, home);
		try {
			var seq = raft.getLogSequence();
			seq.trySetTerm(1);
			var stateField = Raft.class.getDeclaredField("state");
			stateField.setAccessible(true);
			stateField.set(raft, Raft.RaftState.Leader);

			long now = System.currentTimeMillis();
			var canceledCount = new AtomicInteger();
			var succeeded = new AtomicBoolean();
			var log1 = new HeartbeatLog();
			log1.getUnique().setClientId("client.trunc");
			log1.getUnique().setRequestId(7);
			log1.setCreateTime(now);
			seq.appendLog(log1, (holder, ok) -> {
				if (ok)
					succeeded.set(true);
				else
					canceledCount.incrementAndGet();
			});

			var log2 = new HeartbeatLog();
			log2.getUnique().setClientId("client.trunc");
			log2.getUnique().setRequestId(8);
			log2.setCreateTime(now);
			seq.appendLog(log2, (holder, ok) -> {
				if (ok)
					succeeded.set(true);
				else
					canceledCount.incrementAndGet();
			});

			raft.lock();
			try {
				var ae = new AppendEntries();
				ae.Argument.setTerm(2);
				ae.Argument.setLeaderId("127.0.0.1:19751");
				ae.Argument.setPrevLogIndex(0);
				ae.Argument.setPrevLogTerm(0);
				ae.Argument.setLeaderCommit(0);
				ae.Argument.getEntries().add(new Binary(new RaftLog(2, 1, new HeartbeatLog()).encode()));
				ae.Argument.setLastEntryIndex(1);

				seq.followerOnAppendEntries(ae);
				assertTrue(ae.Result.getSuccess());
				assertEquals(1, seq.getLastIndex());
				var newLog1 = seq.readLog(1);
				assertNotNull(newLog1);
				assertEquals(2, newLog1.getTerm());
				assertNull(seq.readLog(2));
				assertFalse(succeeded.get());
				assertEquals(2, canceledCount.get(), "被截断的未提交日志回调必须全部取消");

				for (long reqId : new long[]{7, 8}) {
					var retry = new Zeze.Builtin.ServiceManagerWithRaft.Login();
					retry.getUnique().setClientId("client.trunc");
					retry.getUnique().setRequestId(reqId);
					retry.setCreateTime(now);
					assertSame(UniqueRequestState.NOT_FOUND, seq.tryGetRequestState(retry),
							"被截断日志的唯一请求存根必须同步删除，不得留孤儿存根");
				}
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}
}
