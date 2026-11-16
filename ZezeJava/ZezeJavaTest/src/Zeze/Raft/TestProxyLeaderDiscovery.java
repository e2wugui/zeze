package Zeze.Raft;

import static java.util.Objects.requireNonNull;

import harness.Extra;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import Zeze.Dbh2.Dbh2;
import Zeze.Net.Acceptor;
import Zeze.Net.ServiceConf;
import Zeze.Raft.RaftConfig.Node;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 变体A首发现（2026-11-16）：ProxyAgent模式下Agent.init全量建连+并行GetLeader问询，
 * 权威leader取代"猜首节点"（ProxyServer为GetLeader预拦截对齐直连语义——follower也能
 * 应答它认知的leader）。本优化只以leader稳定态为目标；冷启动（选举未决）不在范围。
 * 拓扑：三节点raft 19270-19272（Dbh2夹具，每节点以节点名命名raft——生产上每manager的
 * 本地raft名=节点名 host_raftPort，ProxyServer按该键路由），ProxyServer acceptor 19275
 * 共置挂全部三raft（生产每manager一个；连接共享粒度由proxy端点决定，发现逻辑一致）。
 */
@Extra
public class TestProxyLeaderDiscovery {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 服务端raft配置（服务端节点不需要Proxy属性；DbHome注入见内）
	private static final String RAFT_SERVER = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19270"/>
				<node Host="127.0.0.1" Port="19271"/>
				<node Host="127.0.0.1" Port="19272"/>
			</raft>
			""";

	// 客户端配置：全节点ProxyHost/ProxyPort指向本JVM的ProxyServer
	private static final String RAFT_CLIENT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19270" ProxyHost="127.0.0.1" ProxyPort="19275"/>
				<node Host="127.0.0.1" Port="19271" ProxyHost="127.0.0.1" ProxyPort="19275"/>
				<node Host="127.0.0.1" Port="19272" ProxyHost="127.0.0.1" ProxyPort="19275"/>
			</raft>
			""";

	private static final int PROXY_PORT = 19275;

	@Test
	public void testInitDiscoversAuthoritativeLeader(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("dbh2TestProxyLeaderDiscovery").toString());
		var nodes = new ArrayList<Dbh2>();
		var config = Zeze.Config.load();
		var svcConf = new ServiceConf();
		svcConf.addAcceptor(new Acceptor(PROXY_PORT, "127.0.0.1"));
		config.getServiceConfMap().put(ProxyServer.eProxyServerName, svcConf);
		var proxyServer = new ProxyServer(config, 3000);
		try {
			// 三节点（照抄TestDbh2MultiBucketWalk.startBucket：每节点独立config+DbHome隔离；
			// raft名=节点名，与客户端wrap的raftName及ProxyServer.addRaft的路由键对齐）
			for (Node node : RaftConfig.loadFromString(RAFT_SERVER).getNodes().values()) {
				var nodeConfig = RAFT_SERVER.replaceFirst("<raft ",
						"<raft DbHome=\"" + Matcher.quoteReplacement(
								String.valueOf(tempDir.resolve(node.getName().replace(':', '_')))) + "\" ");
				var dbh2 = new Dbh2(null, node.getName(), database,
						RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne);
				nodes.add(dbh2);
				proxyServer.addRaft(dbh2.getRaft());
			}
			proxyServer.start();

			// 等选出稳定leader（测试前置，非被测逻辑）
			var leaderDeadline = System.currentTimeMillis() + 15_000;
			String leaderName = null;
			while (leaderName == null && System.currentTimeMillis() < leaderDeadline) {
				for (var n : nodes)
					if (n.getRaft().isLeader()) {
						leaderName = n.getRaft().getName();
						break;
					}
				if (leaderName == null)
					//noinspection BusyWait
					Thread.sleep(50);
			}
			Assertions.assertNotNull(leaderName, "15s内未选出leader");

			var proxyAgent = new ProxyAgent(3000);
			try {
				// 预建共享连接到ready：发现轮的连接建立宽限在生产是尽力而为（未ready本轮跳过），
				// 测试要确定性问询，先让ConnectorEx ready再建agent。
				var firstNode = RaftConfig.loadFromString(RAFT_CLIENT).getNodes().values().iterator().next();
				var conn = requireNonNull(proxyAgent.getLeader(firstNode));
				var readyDeadline = System.currentTimeMillis() + 5_000;
				while (conn.getConnector().TryGetReadySocket() == null
						&& System.currentTimeMillis() < readyDeadline)
					//noinspection BusyWait
					Thread.sleep(20);
				Assertions.assertNotNull(conn.getConnector().TryGetReadySocket(), "proxy连接5s未ready");

				var agent = new Agent("test.proxyLeaderDiscovery",
						RaftConfig.loadFromString(RAFT_CLIENT), null, proxyAgent);
				try {
					// 权威发现的判据：term>0（初始猜测态term恒0）且leader指向真实leader节点
					//（与首节点猜测无关——即便leader恰为首节点，term也能区分发现与猜测）。
					Assertions.assertTrue(agent.getTerm() > 0,
							"首发现应拿到权威term，实际=" + agent.getTerm());
					Assertions.assertEquals(leaderName, requireNonNull(agent.getLeader()).getName());
				} finally {
					agent.stop();
				}
			} finally {
				proxyAgent.stop();
			}
		} finally {
			proxyServer.stop();
			for (var n : nodes) {
				n.close();
				// 目录最终由@TempDir清理；close后先删一次给Windows句柄延迟释放留缓冲（夹具惯例）
				LogSequence.deleteDirectory(new File(requireNonNull(n.getRaft().getRaftConfig().getDbHome())));
			}
			database.close();
		}
	}
}
