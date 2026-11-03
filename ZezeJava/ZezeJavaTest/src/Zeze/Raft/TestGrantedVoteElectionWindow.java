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
}
