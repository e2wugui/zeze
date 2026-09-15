package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import Zeze.Raft.Agent;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND5-32 回归：raft版SM leader切换后，无周期raft请求的纯订阅者感知不到新leader
 * （LeaderIs仅在请求被重定向时触达此类agent），会话留在旧leader上，新leader的
 * reconcileSessions按GetSocket判活把行当死会话清除——Edit推送从此静默中断，
 * 无报错无恢复，直至进程重启。
 * 场景：3节点集群，订阅者登录并收到首个Edit → 停leader的server（余两节点选出
 * 新leader）→ 断言订阅者agent的raft leader迁到新leader → 反射直调新leader的
 * reconcileSessions（跳过60s周期，确定性走判死路径）→ 新agent注册新实例 →
 * 断言订阅者仍收到Edit（推送链未中断）。
 * 基建对齐 TestServiceManagerWithRaftEditRollback（3节点进程内raft）。
 */
@Fast
public class TestServiceManagerWithRaftLeaderSwitchSubscribe {
	private static final String RAFT_NAME = "fnd5_32_sm_test";
	private static final String SERVICE_NAME = "Fnd5LeaderSwitch";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (Zeze.Net.Selectors.getInstance().getCount() < cpuCount)
			Zeze.Net.Selectors.getInstance().add(cpuCount - Zeze.Net.Selectors.getInstance().getCount());
		var used = new java.util.HashSet<Integer>();
		for (int i = 0; i < ports.length; i++)
			while (true) {
				try (var socket = new ServerSocket(0)) {
					if (used.add(socket.getLocalPort())) {
						ports[i] = socket.getLocalPort();
						break;
					}
				}
			}
		raftXmlFile = Files.createTempFile(RAFT_NAME, ".xml");
		var raftXml = raftXmlString();
		Files.writeString(raftXmlFile, raftXml);
		var nodeNames = new ArrayList<String>();
		for (int i = 0; i < ports.length; i++) {
			var raftConf = RaftConfig.loadFromString(raftXml);
			for (var node : raftConf.getNodes().values())
				if (node.getPort() == ports[i])
					nodeNames.add(node.getName());
			servers.add(new ServiceManagerWithRaft(nodeNames.get(i), raftConf, new Config(), false));
			dbHomes.add(raftConf.getDbHome());
		}
		var rocksField = ServiceManagerWithRaft.class.getDeclaredField("rocks");
		rocksField.setAccessible(true);
		for (var server : servers)
			rocksList.add((Rocks)rocksField.get(server));
		waitStableLeaderExcept(-1);
	}

	@AfterAll
	@Timeout(90) // 生命周期方法默认不受类/方法级超时保护，显式兜底防拆集群挂起拖死suite
	public static void tearDown() throws Exception {
		for (var rocks : rocksList) {
			try {
				rocks.getRaft().getServer().stop();
			} catch (Throwable ignored) {
			}
		}
		for (var server : servers)
			server.close();
		servers.clear();
		Files.deleteIfExists(raftXmlFile);
		for (var dbHome : dbHomes)
			LogSequence.deleteDirectory(new java.io.File(dbHome));
		dbHomes.clear();
	}

	@Test
	@Timeout(200)
	public void testSubscriberSurvivesLeaderSwitch() throws Exception {
		var seenIdentities = new ConcurrentLinkedQueue<String>();
		var agent = newAgent("UnitTest.FND5_32.Subscriber");
		try {
			agent.start();
			agent.waitReady();
			agent.setOnChanged(edit -> {
				for (var add : edit.getAdd())
					if (SERVICE_NAME.equals(add.getServiceName()))
						seenIdentities.add(add.getServiceIdentity());
			});
			var subArg = new BSubscribeArgument();
			subArg.subs.add(new BSubscribeInfo(SERVICE_NAME));
			agent.subscribeServices(subArg);

			// 前置：切换前Edit推送可达。
			var registrar1 = newAgent("UnitTest.FND5_32.Reg1");
			try {
				registrar1.start();
				registrar1.waitReady();
				registrar1.registerService(new BServiceInfo(SERVICE_NAME, "1", 0, "127.0.0.1", 4231));
				waitIdentity(seenIdentities, "1", 10_000, "前置：切换前订阅者必须收到Edit");
			} finally {
				registrar1.close();
			}

			// 停当前leader（订阅者会话所在节点）：余两节点选出新leader。
			var oldLeaderIdx = currentLeaderIdx();
			Assertions.assertTrue(oldLeaderIdx >= 0, "应有leader");
			rocksList.get(oldLeaderIdx).getRaft().getServer().stop();
			try {
				waitStableLeaderExcept(oldLeaderIdx);
				// 必须排除被stop的节点：raft对象未shutdown，服务停了收不到更高term，
				// 其状态机永远自认Leader（僵尸）。不排除时若僵尸下标较小会被先扫到，
				// 误判"未选出新leader"（~20%假红）。
				var newLeaderIdx = currentLeaderIdx(oldLeaderIdx);
				Assertions.assertTrue(newLeaderIdx >= 0 && newLeaderIdx != oldLeaderIdx, "应选出新leader");
				var newLeaderName = rocksList.get(newLeaderIdx).getRaft().getName();

				// FND5-32核心断言：订阅者必须感知新leader（探测周期30s，含迁移余量给45s）。
				long deadline = System.currentTimeMillis() + 45_000;
				while (!newLeaderName.equals(agentLeaderName(agent))) {
					Assertions.assertTrue(System.currentTimeMillis() < deadline,
							"leader切换后订阅者必须感知新leader（FND5-32）");
					//noinspection BusyWait
					Thread.sleep(200);
				}

				// 确定性走判死路径：反射直调新leader的reconcileSessions（周期60s）。
				// 迁移完成后行必须存活：GetSocket(新sessionId)非空，不会被清除。
				var reconcile = ServiceManagerWithRaft.class.getDeclaredMethod("reconcileSessions");
				reconcile.setAccessible(true);
				reconcile.invoke(servers.get(newLeaderIdx));

				var registrar2 = newAgent("UnitTest.FND5_32.Reg2");
				try {
					registrar2.start();
					registrar2.waitReady();
					registrar2.registerService(new BServiceInfo(SERVICE_NAME, "2", 0, "127.0.0.1", 4232));
					waitIdentity(seenIdentities, "2", 10_000,
							"leader切换+对账后订阅者必须仍能收到Edit（FND5-32）");
				} finally {
					registrar2.close();
				}
			} finally {
				rocksList.get(oldLeaderIdx).getRaft().getServer().start();
			}
		} finally {
			agent.close();
			waitSessionCleaned("UnitTest.FND5_32.Subscriber");
		}
	}

	private static ServiceManagerAgentWithRaft newAgent(String sessionName) throws Exception {
		var config = Config.load();
		config.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		config.getServiceManagerConf().setSessionName(sessionName);
		return new ServiceManagerAgentWithRaft(config);
	}

	/** 订阅者agent的raft leader名（未选出为null）。 */
	private static String agentLeaderName(ServiceManagerAgentWithRaft agent) throws Exception {
		Field f = ServiceManagerAgentWithRaft.class.getDeclaredField("raftClient");
		f.setAccessible(true);
		var leader = ((Agent)f.get(agent)).getLeader();
		return leader != null ? leader.getName() : null;
	}

	private static void waitIdentity(ConcurrentLinkedQueue<String> seen, String identity,
									 long timeoutMs, String message) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (!seen.contains(identity)) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message);
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}

	private static int currentLeaderIdx() {
		return currentLeaderIdx(-1);
	}

	/** exceptIdx>=0时排除该节点：被stop的raft对象仍自认Leader（僵尸），见调用处注释。 */
	private static int currentLeaderIdx(int exceptIdx) {
		for (int i = 0; i < rocksList.size(); i++)
			if (i != exceptIdx && rocksList.get(i).isLeader())
				return i;
		return -1;
	}

	/** 定界等待（≤60s）三个节点的tSession均无指定会话行；超时仅记录，teardown由@Timeout兜底。 */
	private static void waitSessionCleaned(String sessionName) throws Exception {
		long deadline = System.currentTimeMillis() + 60_000;
		while (System.currentTimeMillis() < deadline) {
			var found = new boolean[]{false};
			for (var server : servers) {
				try {
					sessionTableOf(server).walk((name, v) -> {
						if (sessionName.equals(name))
							found[0] = true;
						return true;
					});
				} catch (Throwable ignored) {
					// 节点重启窗口/无leader时walk失败：继续轮询
				}
			}
			if (!found[0])
				return;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		System.err.println("FND5-32 test: session row not cleaned in 60s, teardown proceeds with @Timeout guard.");
	}

	@SuppressWarnings("unchecked")
	private static Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession> sessionTableOf(
			ServiceManagerWithRaft server) throws Exception {
		Field f = ServiceManagerWithRaft.class.getDeclaredField("tableSession");
		f.setAccessible(true);
		return (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession>)f.get(server);
	}

	private static String raftXmlString() {
		var sb = new StringBuilder("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="%s">
				""".formatted(RAFT_NAME));
		for (int port : ports)
			sb.append("\t<node Host=\"127.0.0.1\" Port=\"").append(port).append("\"/>\n");
		return sb.append("</raft>\n").toString();
	}

	private static void waitStableLeaderExcept(int exceptIdx) throws Exception {
		long deadline = System.currentTimeMillis() + 90_000;
		long stableSince = 0;
		int stableIdx = -1;
		while (System.currentTimeMillis() < deadline) {
			int leaderIdx = -1;
			for (int i = 0; i < rocksList.size(); i++)
				if (i != exceptIdx && rocksList.get(i).isLeader())
					leaderIdx = i;
			long now = System.currentTimeMillis();
			if (leaderIdx < 0)
				stableIdx = -1;
			else if (leaderIdx != stableIdx) {
				stableIdx = leaderIdx;
				stableSince = now;
			} else if (now - stableSince >= 5_000)
				return;
			//noinspection BusyWait
			Thread.sleep(100);
		}
		Assertions.fail("90s内未出现稳定leader");
	}
}
