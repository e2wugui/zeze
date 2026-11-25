package Zeze.Raft;

import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestLeaderTransitionClearsPending {
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

	@Test
	void stepDownAndReelectionClearConnectorPending(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var raft = newRaft(19800, home);
		try {
			var connectors = new ArrayList<Server.ConnectorEx>();
			raft.getServer().getConfig().forEachConnector(c -> {
				c.setAutoReconnect(false);
				connectors.add((Server.ConnectorEx)c);
			});
			assertEquals(2, connectors.size());

			var stateField = Raft.class.getDeclaredField("state");
			stateField.setAccessible(true);

			raft.lock();
			try {
				// 1. Leader -> Follower 退位必须清空所有连接器的 pending。
				stateField.set(raft, Raft.RaftState.Leader);
				for (var c : connectors)
					c.setPending(new AppendEntries());

				raft.convertStateTo(Raft.RaftState.Follower);
				for (var c : connectors)
					assertNull(c.getPending(), "Leader 退位后必须清空 ConnectorEx.pending");

				// 2. Candidate -> Leader 重当选（纵深防御）也必须清理任何残留的旧 pending。
				stateField.set(raft, Raft.RaftState.Candidate);
				var stale = new AppendEntries();
				for (var c : connectors)
					c.setPending(stale);

				raft.convertStateTo(Raft.RaftState.Leader);
				for (var c : connectors)
					assertNotSame(stale, c.getPending(), "Candidate->Leader 重当选后不得残留旧任期 pending");
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}
}
