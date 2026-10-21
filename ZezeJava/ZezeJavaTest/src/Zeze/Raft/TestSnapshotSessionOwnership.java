package Zeze.Raft;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSnapshotSessionOwnership {

	private static final class Fixture implements AutoCloseable {
		final Raft raft;
		final LogSequence logs;
		final Server.ConnectorEx peer = new Server.ConnectorEx("127.0.0.1", 19521);
		final InstallSnapshotState oldSession;
		final InstallSnapshotState currentSession;
		final RandomAccessFile currentFile;

		Fixture(Path home) throws Exception {
			Task.tryInitThreadPool();
			// 只构造本地 Raft 与真实快照句柄；不启动服务、不绑定端口，无 Application。
			var conf = RaftConfig.loadFromString("""
					<raft Name="127.0.0.1:19520" DbHome="%s">
					    <node Host="127.0.0.1" Port="19520"/>
					    <node Host="127.0.0.1" Port="19521"/>
					    <node Host="127.0.0.1" Port="19522"/>
					</raft>
					""".formatted(home));
			raft = new Raft(new StateMachine() {
				@Override
				public SnapshotResult snapshot(String path) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void loadSnapshot(String path) {
				}
			}, null, conf, new Config());
			logs = raft.getLogSequence();
			logs.trySetTerm(1);
			var state = Raft.class.getDeclaredField("state");
			state.setAccessible(true);
			state.set(raft, Raft.RaftState.Leader);
			var file = Files.write(home.resolve("snapshot-test.bin"), new byte[10]).toFile();
			oldSession = session(new RandomAccessFile(file, "r"));
			logs.getSendSnapshotting().put(peer.getName(), oldSession);
			// 同任期断线/重试即可替换会话，无需假定选举；旧 RPC 回调还在执行队列中。
			logs.getSendSnapshotting().end(peer);
			currentFile = new RandomAccessFile(file, "r");
			currentSession = session(currentFile);
			logs.getSendSnapshotting().put(peer.getName(), currentSession);
			peer.setNextIndex(2);
			peer.setMatchIndex(1);
		}

		private InstallSnapshotState session(RandomAccessFile file) {
			var session = new InstallSnapshotState();
			session.setConnector(peer);
			session.setFile(file);
			session.setTerm(logs.getTerm());
			session.setLeaderId(raft.getName());
			session.setLastIncludedIndex(6);
			session.setLastIncludedTerm(1);
			session.setFirstLog(new RaftLog(1, 6, new HeartbeatLog()));
			return session;
		}

		InstallSnapshot response() {
			var rpc = new InstallSnapshot();
			rpc.Argument.setTerm(1);
			rpc.Argument.setDone(true);
			rpc.Result.setTerm(1);
			rpc.Result.setOffset(-1);
			return rpc;
		}

		void deliver(InstallSnapshotState session, InstallSnapshot rpc) throws Exception {
			var method = InstallSnapshotState.class.getDeclaredMethod("processResult",
					LogSequence.class, Server.ConnectorEx.class, Protocol.class);
			method.setAccessible(true);
			method.invoke(session, logs, peer, rpc);
		}

		void assertCurrentUntouched() throws Exception {
			assertTrue(logs.getSendSnapshotting().contains(peer.getName()), "旧事件不能移除新会话");
			assertTrue(currentFile.getFD().valid(), "新会话的文件必须仍然可读");
			assertEquals(0, currentFile.getFilePointer());
			assertEquals(0, currentSession.getOffset());
			assertFalse(currentSession.getDone());
			assertEquals(2, peer.getNextIndex());
			assertEquals(1, peer.getMatchIndex());
		}

		@Override
		public void close() throws Exception {
			try {
				raft.shutdown();
			} finally {
				currentFile.close();
			}
		}
	}

	@Test
	void oldTimeoutKeepsReplacement(@TempDir Path home) throws Exception {
		try (var f = new Fixture(home)) {
			var rpc = f.response();
			rpc.setIsTimeout(true);
			f.deliver(f.oldSession, rpc);
			f.assertCurrentUntouched();
		}
	}

	@Test
	void oldFailureKeepsReplacement(@TempDir Path home) throws Exception {
		try (var f = new Fixture(home)) {
			var rpc = f.response();
			rpc.setResultCode(Procedure.Exception);
			f.deliver(f.oldSession, rpc);
			f.assertCurrentUntouched();
		}
	}

	@Test
	void oldSuccessKeepsReplacement(@TempDir Path home) throws Exception {
		try (var f = new Fixture(home)) {
			f.deliver(f.oldSession, f.response());
			f.assertCurrentUntouched();
		}
	}

	@Test
	void oldContinuationKeepsReplacement(@TempDir Path home) throws Exception {
		try (var f = new Fixture(home)) {
			f.oldSession.trySend(f.logs, f.peer);
			f.assertCurrentUntouched();
		}
	}

	@Test
	void obsoleteResponseStillObservesHigherTerm(@TempDir Path home) throws Exception {
		try (var f = new Fixture(home)) {
			var rpc = f.response();
			rpc.Result.setTerm(2);
			f.deliver(f.oldSession, rpc);
			assertEquals(2, f.logs.getTerm());
			assertFalse(f.raft.isLeader());
			assertTrue(f.logs.getSendSnapshotting().isEmpty());
			assertFalse(f.currentFile.getFD().valid());
		}
	}

	@Test
	void currentSuccessAdvancesReplication(@TempDir Path home) throws Exception {
		try (var f = new Fixture(home)) {
			f.deliver(f.currentSession, f.response());
			assertTrue(f.logs.getSendSnapshotting().isEmpty());
			assertFalse(f.currentFile.getFD().valid());
			assertEquals(7, f.peer.getNextIndex());
			assertEquals(6, f.peer.getMatchIndex());
		}
	}
}
