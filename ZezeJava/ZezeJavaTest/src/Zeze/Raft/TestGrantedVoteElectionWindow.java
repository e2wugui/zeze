package Zeze.Raft;

import java.nio.file.Path;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestGrantedVoteElectionWindow {
	@Test
	void grantsRefreshTimerAndRejectionsDoNotDelayElection(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var config = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19610" DbHome="%s">
				  <node Host="127.0.0.1" Port="19610"/>
				  <node Host="127.0.0.1" Port="19611"/>
				  <node Host="127.0.0.1" Port="19612"/>
				</raft>
				""".formatted(home));
		var raft = new Raft(new StateMachine() {
			@Override public SnapshotResult snapshot(String path) { throw new UnsupportedOperationException(); }
			@Override public void loadSnapshot(String path) { }
		}, null, config, new Config());
		try {
			raft.lock();
			try {
				var sequence = raft.getLogSequence();
				sequence.trySetTerm(2);
				sequence.setLeaderActiveTime(1);
				var process = Raft.class.getDeclaredMethod("processRequestVote", RequestVote.class);
				process.setAccessible(true);
				var vote = new RequestVote();
				vote.Argument.setTerm(2);
				vote.Argument.setCandidateId("127.0.0.1_19611");
				long before = System.currentTimeMillis();
				process.invoke(raft, vote);
				assertTrue(vote.Result.getVoteGranted());
				assertTrue(sequence.getLeaderActiveTime() >= before);
				var timer = Raft.class.getDeclaredField("nextVoteTime");
				timer.setAccessible(true);
				assertEquals(sequence.getLeaderActiveTime() + sequence.getElectionTimeout(), timer.getLong(raft));
				var state = Raft.class.getDeclaredField("state");
				state.setAccessible(true);
				state.set(raft, Raft.RaftState.Candidate);
				timer.setLong(raft, 7);
				sequence.setLeaderActiveTime(1);
				var rejected = new RequestVote();
				rejected.Argument.setTerm(2);
				rejected.Argument.setCandidateId("127.0.0.1_19612");
				process.invoke(raft, rejected);
				assertFalse(rejected.Result.getVoteGranted());
				assertEquals(1, sequence.getLeaderActiveTime());
				assertEquals(7, timer.getLong(raft));
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void higherTermRejectedVoteDoesNotResetElectionTimer(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var config = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19620" DbHome="%s">
				  <node Host="127.0.0.1" Port="19620"/>
				  <node Host="127.0.0.1" Port="19621"/>
				  <node Host="127.0.0.1" Port="19622"/>
				</raft>
				""".formatted(home));
		var raft = new Raft(new StateMachine() {
			@Override public SnapshotResult snapshot(String path) { throw new UnsupportedOperationException(); }
			@Override public void loadSnapshot(String path) { }
		}, null, config, new Config());
		try {
			raft.lock();
			try {
				var sequence = raft.getLogSequence();
				sequence.trySetTerm(2);
				sequence.saveLog(new RaftLog(2, 1, new HeartbeatLog()));
				var lastIndexField = LogSequence.class.getDeclaredField("lastIndex");
				lastIndexField.setAccessible(true);
				lastIndexField.setLong(sequence, 1);

				var process = Raft.class.getDeclaredMethod("processRequestVote", RequestVote.class);
				process.setAccessible(true);

				// Follower 收到更高任期但日志落后的 RequestVote：更新 term 但拒投，不得重置 leaderActiveTime。
				sequence.setLeaderActiveTime(1);
				var staleFollowerVote = new RequestVote();
				staleFollowerVote.Argument.setTerm(3);
				staleFollowerVote.Argument.setCandidateId("127.0.0.1_19621");
				staleFollowerVote.Argument.setLastLogIndex(0);
				staleFollowerVote.Argument.setLastLogTerm(0);
				process.invoke(raft, staleFollowerVote);
				assertFalse(staleFollowerVote.Result.getVoteGranted());
				assertEquals(3, sequence.getTerm());
				assertEquals(1, sequence.getLeaderActiveTime(), "更高任期被拒 RequestVote 不得刷新 Follower 选举计时");

				// Candidate 收到更高任期但日志落后的 RequestVote：退回 Follower 且拒投，不得重置 leaderActiveTime。
				var stateField = Raft.class.getDeclaredField("state");
				stateField.setAccessible(true);
				stateField.set(raft, Raft.RaftState.Candidate);
				sequence.setLeaderActiveTime(1);
				var staleCandidateVote = new RequestVote();
				staleCandidateVote.Argument.setTerm(4);
				staleCandidateVote.Argument.setCandidateId("127.0.0.1_19622");
				staleCandidateVote.Argument.setLastLogIndex(0);
				staleCandidateVote.Argument.setLastLogTerm(0);
				process.invoke(raft, staleCandidateVote);
				assertFalse(staleCandidateVote.Result.getVoteGranted());
				assertEquals(4, sequence.getTerm());
				assertEquals(Raft.RaftState.Follower, raft.getState());
				assertEquals(1, sequence.getLeaderActiveTime(), "更高任期被拒 RequestVote 不得刷新 Candidate 选举计时");

				// Leader -> Follower 退位必须刷新 leaderActiveTime（Leader 期间不维护该计时）。
				stateField.set(raft, Raft.RaftState.Leader);
				sequence.setLeaderActiveTime(1);
				long beforeStepDown = System.currentTimeMillis();
				raft.convertStateTo(Raft.RaftState.Follower);
				assertTrue(sequence.getLeaderActiveTime() >= beforeStepDown, "Leader 退位必须刷新 leaderActiveTime");
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}
}
