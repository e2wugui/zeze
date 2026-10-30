package Zeze.Raft;

import java.nio.file.Path;
import java.util.List;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Rpc;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestVoteResponseTerm {
	private static Raft createRaft(Path home) throws Exception {
		Task.tryInitThreadPool();
		// 只构造真实日志库，不启动 server、不绑定端口，无 Application 或共享内存库。
		var config = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19610" DbHome="%s">
					<node Host="127.0.0.1" Port="19610"/>
					<node Host="127.0.0.1" Port="19611"/>
					<node Host="127.0.0.1" Port="19612"/>
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
		}, null, config, new Config());
	}

	private static void prepare(Raft raft, Raft.RaftState role) throws Exception {
		raft.getLogSequence().trySetTerm(2);
		raft.getLogSequence().setVoteFor(raft.getName());
		var state = Raft.class.getDeclaredField("state");
		state.setAccessible(true);
		state.set(raft, role);
	}

	private static void deliver(Raft raft, Rpc<BRequestVoteArgument, BRequestVoteResult> rpc) throws Exception {
		var method = Raft.class.getDeclaredMethod(rpc instanceof PreVote
				? "processPreVoteResult" : "processRequestVoteResult", rpc.getClass(), Connector.class);
		method.setAccessible(true);
		method.invoke(raft, rpc, null);
	}

	private static void checkHigherTerm(Path home, Raft.RaftState role,
									   Rpc<BRequestVoteArgument, BRequestVoteResult> rpc) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				prepare(raft, role);
				rpc.Argument.setTerm(1); // 上轮选举请求已过期且不在当前投票集合。
				rpc.Result.setTerm(3);
				deliver(raft, rpc);
				assertEquals(3, raft.getLogSequence().getTerm(), "迟到应答的更高任期仍须被采纳");
				assertEquals(Raft.RaftState.Follower, raft.getState());
				assertEquals("", raft.getLogSequence().getVoteFor());
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
		var reopened = createRaft(home);
		try {
			assertEquals(3, reopened.getLogSequence().getTerm(), "任期必须持久化");
			assertEquals("", reopened.getLogSequence().getVoteFor(), "新任期的旧投票必须清空");
		} finally {
			reopened.shutdown();
		}
	}

	@ParameterizedTest
	@EnumSource(Raft.RaftState.class)
	void obsoleteRequestVoteStillAdoptsHigherTerm(Raft.RaftState role, @TempDir Path home) throws Exception {
		checkHigherTerm(home, role, new RequestVote());
	}

	@ParameterizedTest
	@EnumSource(Raft.RaftState.class)
	void obsoletePreVoteStillAdoptsHigherTerm(Raft.RaftState role, @TempDir Path home) throws Exception {
		checkHigherTerm(home, role, new PreVote());
	}

	@Test
	void obsoleteGrantsCannotElectCandidate(@TempDir Path home) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				prepare(raft, Raft.RaftState.Candidate);
				for (var vote : List.of(new RequestVote(), new PreVote())) {
					vote.Argument.setTerm(1);
					vote.Result.setTerm(2);
					vote.Result.setVoteGranted(true);
					deliver(raft, vote);
					assertEquals(2, raft.getLogSequence().getTerm());
					assertEquals(Raft.RaftState.Candidate, raft.getState());
				}
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void timeoutDoesNotAdoptUnreceivedTerm(@TempDir Path home) throws Exception {
		var raft = createRaft(home);
		try {
			raft.lock();
			try {
				prepare(raft, Raft.RaftState.Leader);
				var vote = new RequestVote();
				vote.setIsTimeout(true);
				vote.Result.setTerm(3);
				deliver(raft, vote);
				var preVote = new PreVote();
				preVote.setIsTimeout(true);
				preVote.Result.setTerm(3);
				deliver(raft, preVote);
				assertEquals(2, raft.getLogSequence().getTerm());
				assertEquals(Raft.RaftState.Leader, raft.getState());
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}
}
