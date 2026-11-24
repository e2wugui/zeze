package Zeze.Raft;

import java.util.List;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestProxyAgentRegistrationFailure {
	@Test
	public void duplicateNodeLeavesEarlierFreeNodeUnregistered() throws Exception {
		Task.tryInitThreadPool();
		var combined = RaftConfig.loadFromSortedNames("-127.0.0.1_19564-127.0.0.1_19565");
		var nodes = List.copyOf(combined.getNodes().keySet());
		var freeConfig = RaftConfig.loadFromSortedNames("-" + nodes.getFirst());
		var occupiedConfig = RaftConfig.loadFromSortedNames("-" + nodes.getLast());
		var proxy = new ProxyAgent(1000);
		var occupied = new Agent("testProxyAgentRegistration.occupied", occupiedConfig, new Config());
		var candidate = new Agent("testProxyAgentRegistration.combined", combined, new Config());
		var free = new Agent("testProxyAgentRegistration.free", freeConfig, new Config());
		try {
			proxy.addAgent(occupied);
			assertThrows(RuntimeException.class, () -> proxy.addAgent(candidate));
			proxy.addAgent(free);
		} finally {
			proxy.removeAgent(free);
			proxy.removeAgent(candidate);
			proxy.removeAgent(occupied);
			free.stop();
			candidate.stop();
			occupied.stop();
			proxy.stop();
		}
	}

	@Test
	public void failedConstructorReleasesItsNodeRegistration() throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = RaftConfig.loadFromSortedNames("-127.0.0.1_19563");
		var proxy = new ProxyAgent(1000);
		var replacement = new Agent("testProxyAgentRegistration.replacement", raftConfig, new Config());
		try {
			assertThrows(IllegalStateException.class,
					() -> new Agent("testProxyAgentRegistration.invalid", raftConfig, new Config(), proxy));
			proxy.addAgent(replacement);
		} finally {
			proxy.removeAgent(replacement);
			replacement.stop();
			proxy.stop();
		}
	}
}
