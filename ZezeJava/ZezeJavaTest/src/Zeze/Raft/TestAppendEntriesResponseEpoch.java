package Zeze.Raft;

import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAppendEntriesResponseEpoch {

	private static Raft createRaft(Path home) throws Exception {
		Task.tryInitThreadPool();
		// 五节点仅用于多数派计算，不启动 server、不绑定端口；无 Application 或共享内存库。
		var conf = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19510" DbHome="%s">
				    <node Host="127.0.0.1" Port="19510"/>
				    <node Host="127.0.0.1" Port="19511"/>
				    <node Host="127.0.0.1" Port="19512"/>
				    <node Host="127.0.0.1" Port="19513"/>
				    <node Host="127.0.0.1" Port="19514"/>
				</raft>
				""".formatted(home));
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

	private static Server.ConnectorEx prepareNewLeadership(Raft raft) throws Exception {
		var logs = raft.getLogSequence();
		// 少数派上的旧分叉被新 leader 覆盖；两个 term 的日志具有相同 index。
		for (int term : new int[]{1, 3}) {
			var append = response(term, true);
			append.Argument.setLeaderId("127.0.0.1:19512");
			append.Argument.getEntries().add(new Binary(new RaftLog(term, 1, new HeartbeatLog()).encode()));
			logs.followerOnAppendEntries(append);
			assertTrue(append.Result.getSuccess());
		}
		// 只安装回调入口所需的 leader 状态，避免真实选举启动网络连接。
		var state = Raft.class.getDeclaredField("state");
		state.setAccessible(true);
		state.set(raft, Raft.RaftState.Leader);
		var peers = new ArrayList<Server.ConnectorEx>();
		raft.getServer().getConfig().forEachConnector(c -> peers.add((Server.ConnectorEx)c));
		for (var peer : peers) {
			peer.setNextIndex(2);
			peer.setMatchIndex(0);
		}
		// 当前日志只有本节点与一个 peer 持有，尚未形成五节点多数派。
		peers.get(1).setMatchIndex(1);
		return peers.get(0);
	}

	private static AppendEntries response(long term, boolean success) {
		var rpc = new AppendEntries();
		rpc.Argument.setTerm(term);
		rpc.Argument.setLastEntryIndex(1);
		rpc.Result.setTerm(term);
		rpc.Result.setSuccess(success);
		return rpc;
	}

	private static void deliver(Raft raft, Server.ConnectorEx peer, AppendEntries rpc) throws Exception {
		var method = LogSequence.class.getDeclaredMethod("processAppendEntriesResult", Server.ConnectorEx.class, Protocol.class);
		method.setAccessible(true);
		method.invoke(raft.getLogSequence(), peer, rpc);
	}

	@Test
	void oldSuccessCannotCommitReplacementLog(@TempDir Path home) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				var peer = prepareNewLeadership(raft);
				var old = response(1, true);
				peer.setPending(old); // 上次领导任期遗留的在途请求。
				deliver(raft, peer, old);
				assertEquals(0, raft.getLogSequence().getCommitIndex(), "旧日志 ACK 不能证明当前同索引日志的多数派");
				assertEquals(0, raft.getLogSequence().getLastApplied());
				assertEquals(0, peer.getMatchIndex());
				assertNull(peer.getPending(), "已经完成的旧请求应释放发送槽");
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void oldFailureCannotBacktrackCurrentRequest(@TempDir Path home) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				var peer = prepareNewLeadership(raft);
				var current = response(3, true);
				peer.setPending(current);
				deliver(raft, peer, response(1, false));
				assertEquals(2, peer.getNextIndex());
				assertSame(current, peer.getPending());
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void currentSuccessCommitsMajority(@TempDir Path home) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				var peer = prepareNewLeadership(raft);
				var current = response(3, true);
				peer.setPending(current);
				deliver(raft, peer, current);
				assertEquals(1, raft.getLogSequence().getCommitIndex());
				assertEquals(1, raft.getLogSequence().getLastApplied());
				assertEquals(1, peer.getMatchIndex());
				assertNull(peer.getPending());
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void obsoleteRequestStillReportsHigherTerm(@TempDir Path home) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				var peer = prepareNewLeadership(raft);
				var old = response(1, false);
				old.Result.setTerm(4);
				deliver(raft, peer, old);
				assertEquals(4, raft.getLogSequence().getTerm());
				assertEquals(Raft.RaftState.Follower, raft.getState());
				assertEquals(0, raft.getLogSequence().getCommitIndex());
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}
}
