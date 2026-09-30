package Zeze.Raft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import Zeze.Util.AtomicFileWriter;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestRaftConfigSaveDefaults {
	@TempDir
	Path directory;

	@Test
	public void testResetDefaultsSurvivesSaveAndReload() throws Exception {
		var path = directory.resolve("raft.xml");
		AtomicFileWriter.replace(path, """
				<raft Name="127.0.0.1:17650" AppendEntriesTimeout="3000"
				 LeaderHeartbeatTimer="3200" ElectionRandomMax="1200"
				 MaxAppendEntriesCount="600" SnapshotLogCount="2000000"
				 SnapshotCommitDelayed="true" PreVote="false"
				 BackgroundApplyCount="600" UniqueRequestExpiredDays="8">
				 <node Host="127.0.0.1" Port="17650"/>
				 <node Host="127.0.0.1" Port="17651"/>
				 <node Host="127.0.0.1" Port="17652"/>
				</raft>
				""".getBytes(StandardCharsets.UTF_8));
		var current = RaftConfig.load(path.toString());
		var defaults = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:17650">
				 <node Host="127.0.0.1" Port="17650"/>
				 <node Host="127.0.0.1" Port="17651"/>
				 <node Host="127.0.0.1" Port="17652"/>
				</raft>
				""");
		current.setAppendEntriesTimeout(defaults.getAppendEntriesTimeout());
		current.setLeaderHeartbeatTimer(defaults.getLeaderHeartbeatTimer());
		current.setElectionRandomMax(defaults.getElectionRandomMax());
		current.setMaxAppendEntriesCount(defaults.getMaxAppendEntriesCount());
		current.setSnapshotLogCount(defaults.getSnapshotLogCount());
		current.setSnapshotCommitDelayed(defaults.isSnapshotCommitDelayed());
		current.setPreVote(defaults.isPreVote());
		current.setBackgroundApplyCount(defaults.getBackgroundApplyCount());
		current.setUniqueRequestExpiredDays(defaults.getUniqueRequestExpiredDays());
		current.save();
		var reloaded = RaftConfig.load(path.toString());
		assertEquals(defaults.getAppendEntriesTimeout(), reloaded.getAppendEntriesTimeout());
		assertEquals(defaults.getLeaderHeartbeatTimer(), reloaded.getLeaderHeartbeatTimer());
		assertEquals(defaults.getElectionRandomMax(), reloaded.getElectionRandomMax());
		assertEquals(defaults.getMaxAppendEntriesCount(), reloaded.getMaxAppendEntriesCount());
		assertEquals(defaults.getSnapshotLogCount(), reloaded.getSnapshotLogCount());
		assertEquals(defaults.isSnapshotCommitDelayed(), reloaded.isSnapshotCommitDelayed());
		assertEquals(defaults.isPreVote(), reloaded.isPreVote());
		assertEquals(defaults.getBackgroundApplyCount(), reloaded.getBackgroundApplyCount());
		assertEquals(defaults.getUniqueRequestExpiredDays(), reloaded.getUniqueRequestExpiredDays());
	}
}
