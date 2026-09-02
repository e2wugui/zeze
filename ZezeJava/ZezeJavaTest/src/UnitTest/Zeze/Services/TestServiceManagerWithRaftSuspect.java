package UnitTest.Zeze.Services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Util.Task;
import harness.Fast;

/**
 * S-4：raft 版 ServiceManager 的 Identify/Suspect（Takeover租约换轨，对齐非raft版）。
 * <p>
 * 断线的会话若曾 Identify 上报 serverId，SM 立即（不延迟、不挑选目标）向其余会话广播
 * Suspect(serverId)；接收方转化为 onSuspect 回调（应用接 takeover.tryTransfer）。
 * 替代旧 OfflineRegister/OfflineNotify/600s延迟通知/NormalClose 全家。
 * <p>
 * 进程内 3 节点 raft 集群 + 两个客户端：Dead(serverId=1) 断线，Watcher(serverId=2) 观察。
 */
@Fast
public class TestServiceManagerWithRaftSuspect {
	private static final String raftXmlString = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="s4_sm_test">
				<node Host="127.0.0.1" Port="19511"/>
				<node Host="127.0.0.1" Port="19512"/>
				<node Host="127.0.0.1" Port="19513"/>
			</raft>
			""";
	private static Path raftXmlFile;
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		cleanDirs();
		raftXmlFile = Files.createTempFile("s4_sm_test_raft", ".xml");
		Files.writeString(raftXmlFile, raftXmlString);
		for (var node : RaftConfig.loadFromString(raftXmlString).getNodes().values())
			servers.add(new ServiceManagerWithRaft(node.getName(),
					RaftConfig.loadFromString(raftXmlString), new Config(), false));
	}

	@AfterAll
	public static void tearDown() throws Exception {
		for (var sm : servers)
			sm.close();
		servers.clear();
		Files.deleteIfExists(raftXmlFile);
		cleanDirs();
	}

	// Windows下启动前清理，保证全新状态；收尾best-effort。
	private static void cleanDirs() {
		LogSequence.deleteDirectory(new File("s4_sm_test"));
		for (int port = 19511; port <= 19513; ++port) {
			LogSequence.deleteDirectory(new File("127.0.0.1_" + port));
			LogSequence.deleteDirectory(new File("s4_sm_test_127.0.0.1_" + port));
		}
	}

	private static ServiceManagerAgentWithRaft newAgent(String sessionName, int serverId) throws Exception {
		var config = Config.load();
		config.setServerId(serverId);
		config.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		config.getServiceManagerConf().setSessionName(sessionName);
		var agent = new ServiceManagerAgentWithRaft(config);
		agent.start();
		agent.waitReady();
		return agent;
	}

	@Test
	@Timeout(120)
	public void testSuspectBroadcastOnDisconnect() throws Exception {
		var suspects = new ConcurrentLinkedQueue<Integer>();
		var watcher = newAgent("UnitTest.S4.Watcher", 2);
		try {
			watcher.setOnSuspect(suspects::add);

			var dead = newAgent("UnitTest.S4.Dead", 1);
			try {
				// 等Identify到达服务端（onLoginSuccess异步发送，localhost往返毫秒级）。
				Thread.sleep(1000);
			} finally {
				// 停掉连接（无NormalClose语义，任何断线都广播）。
				dead.close();
			}

			long deadline = System.currentTimeMillis() + 10_000;
			while (suspects.isEmpty() && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertEquals(1, suspects.poll(), "watcher must receive Suspect for dead serverId=1");
			Assertions.assertNull(suspects.poll(), "no extra Suspect expected");
		} finally {
			watcher.close();
		}
	}
}
