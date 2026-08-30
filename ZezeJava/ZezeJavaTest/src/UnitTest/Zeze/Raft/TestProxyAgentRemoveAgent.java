package UnitTest.Zeze.Raft;

import harness.Fast;
import Zeze.Raft.Agent;
import Zeze.Raft.ProxyAgent;
import Zeze.Raft.RaftConfig;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;

/**
 * ProxyAgent 的 agent 生命周期：Agent.stop 必须从进程级单例注销节点注册，
 * 否则同一 JVM 内重建同配置的 agent（环境重建/IDE重跑）命中
 * "duplicate agent node"（2026-08-30 Dbh2FullTest 故障根因）。
 */
@Fast
public class TestProxyAgentRemoveAgent {
	@Test
	public void testStopThenRecreate() throws Exception {
		Task.tryInitThreadPool();
		var raftConfigString = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19550" ProxyHost="127.0.0.1" ProxyPort="19560"/>
					<node Host="127.0.0.1" Port="19551" ProxyHost="127.0.0.1" ProxyPort="19560"/>
				</raft>
				""";
		var proxyAgent = new ProxyAgent(1000);
		try {
			var agent1 = new Agent("testProxyAgentRemove.1",
					RaftConfig.loadFromString(raftConfigString), null, proxyAgent);
			agent1.stop(); // 注销后，才能用同一raft配置重建agent
			var agent2 = new Agent("testProxyAgentRemove.2",
					RaftConfig.loadFromString(raftConfigString), null, proxyAgent);
			agent2.stop();
		} finally {
			proxyAgent.stop();
		}
	}
}
