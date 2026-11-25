package Zeze.Raft;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSentinelLogTermOnRecovery {
	private static RaftConfig newConfig(int portBase, Path home) {
		var name = "127.0.0.1:" + portBase;
		return RaftConfig.loadFromString("""
				<raft Name="%s" DbHome="%s">
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(name, home, portBase, portBase + 1, portBase + 2));
	}

	private static StateMachine newStateMachine() {
		return new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
	}

	private static void writeLong(RocksDatabase.Table table, byte[] key, long value) throws Exception {
		var bb = ByteBuffer.Allocate(9);
		bb.WriteLong(value);
		table.put(RocksDatabase.getDefaultWriteOptions(), key, 0, key.length, bb.Bytes, 0, bb.WriteIndex);
	}

	private static void writeBool(RocksDatabase.Table table, byte[] key, boolean value) throws Exception {
		var bb = ByteBuffer.Allocate(1);
		bb.WriteBool(value);
		table.put(RocksDatabase.getDefaultWriteOptions(), key, 0, key.length, bb.Bytes, 0, bb.WriteIndex);
	}

	@Test
	void emptyLogsWithPersistedTermWritesZeroSentinelAndLosesVote(@TempDir Path dir) throws Exception {
		Task.tryInitThreadPool();
		var recoveringHome = dir.resolve("recovering");
		var voterHome = dir.resolve("voter");
		Files.createDirectories(recoveringHome);
		Files.createDirectories(voterHome);
		var recoveringName = "127.0.0.1_19710";

		// 模拟节点持久化了 term=5 与 nodeReady=true 后在 logs.drop() 与 saveLog 之间崩溃。
		try (var db = new RocksDatabase(recoveringHome.resolve("db").toString())) {
			var rafts = db.getOrAddTable(recoveringName + ".rafts");
			writeLong(rafts, LogSequence.makeRaftsKey(0), 5);
			writeLong(rafts, LogSequence.makeRaftsKey(2), -1);
			writeBool(rafts, LogSequence.makeRaftsKey(3), true);
			db.getOrAddTable(recoveringName + ".logs");
		}

		var recovering = new Raft(newStateMachine(), null, newConfig(19710, recoveringHome), new Config());
		var voter = new Raft(newStateMachine(), null, newConfig(19720, voterHome), new Config());
		try {
			var recSeq = recovering.getLogSequence();
			assertEquals(5, recSeq.getTerm());
			assertTrue(recSeq.getNodeReady());
			var sentinel = recSeq.readLog(0);
			assertNotNull(sentinel);
			assertEquals(0, sentinel.getTerm(), "空日志哨兵 term 必须恒为 0");
			assertEquals(0, recSeq.lastRaftLogTermIndex().getTerm());

			// 构造持有 (term=4, index=100) 且 nodeReady=true 的健康投票者，验证恢复出的空节点无法赢票。
			voter.lock();
			try {
				var voterSeq = voter.getLogSequence();
				voterSeq.trySetTerm(4);
				var trySetNodeReady = LogSequence.class.getDeclaredMethod("trySetNodeReady");
				trySetNodeReady.setAccessible(true);
				trySetNodeReady.invoke(voterSeq);
				voterSeq.saveLog(new RaftLog(4, 100, new HeartbeatLog()));
				var lastIndexField = LogSequence.class.getDeclaredField("lastIndex");
				lastIndexField.setAccessible(true);
				lastIndexField.setLong(voterSeq, 100);

				var req = new RequestVote();
				req.Argument.setTerm(recSeq.getTerm());
				req.Argument.setCandidateId(recovering.getName());
				var recLast = recSeq.lastRaftLogTermIndex();
				req.Argument.setLastLogIndex(recLast.getIndex());
				req.Argument.setLastLogTerm(recLast.getTerm());
				req.Argument.setNodeReady(recSeq.getNodeReady());

				var processVote = Raft.class.getDeclaredMethod("processRequestVote", RequestVote.class);
				processVote.setAccessible(true);
				processVote.invoke(voter, req);
				assertFalse(req.Result.getVoteGranted(), "空日志恢复节点不得击败持有已提交日志的健康节点");
			} finally {
				voter.unlock();
			}
		} finally {
			try {
				recovering.shutdown();
			} finally {
				voter.shutdown();
			}
		}
	}

	@Test
	void halfInstalledLogsMissingBoundaryResetsToZeroTermSentinel(@TempDir Path dir) throws Exception {
		Task.tryInitThreadPool();
		var home = dir.resolve("half-installed");
		Files.createDirectories(home);
		var raftName = "127.0.0.1_19730";

		// 模拟边界日志 saveLog(10) 后、saveFirstIndex(10) 前崩溃：firstIndex=0 缺失且 logs={10}。
		try (var db = new RocksDatabase(home.resolve("db").toString())) {
			var rafts = db.getOrAddTable(raftName + ".rafts");
			writeLong(rafts, LogSequence.makeRaftsKey(0), 5);
			writeLong(rafts, LogSequence.makeRaftsKey(2), 0);
			writeBool(rafts, LogSequence.makeRaftsKey(3), true);
			var logs = db.getOrAddTable(raftName + ".logs");
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(10);
			var val = new RaftLog(4, 10, new HeartbeatLog()).encode();
			logs.put(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex, val.Bytes, 0, val.WriteIndex);
		}

		var raft = new Raft(newStateMachine(), null, newConfig(19730, home), new Config());
		try {
			var seq = raft.getLogSequence();
			assertEquals(5, seq.getTerm());
			assertEquals(0, seq.getFirstIndex());
			assertEquals(0, seq.getLastIndex());
			var sentinel = seq.readLog(0);
			assertNotNull(sentinel);
			assertEquals(0, sentinel.getTerm(), "空洞自愈重建的 index=0 哨兵 term 必须恒为 0");
		} finally {
			raft.shutdown();
		}
	}
}
