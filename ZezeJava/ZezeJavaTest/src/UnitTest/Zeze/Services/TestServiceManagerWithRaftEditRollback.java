package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.LogSequence;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND5-31 回归：raft版editService本地先改、远端失败不回滚——remove失败的条目本地已删、
 * 服务端永续残留（重放只有add语义），僵尸注册持续分发流量直到断连。
 * 场景：agent向3节点集群注册成功 → 停两个follower失去多数派（leader保持、Edit无法提交）
 * → 注销/注册均失败抛错 → 断言本地registers已回滚（注销的条目仍在、失败的注册未落）。
 * 基建对齐 TestServiceManagerWithRaftSessionCloseRetry（3节点进程内raft）。
 */
@Fast
public class TestServiceManagerWithRaftEditRollback {
	private static final String RAFT_NAME = "fnd5_31_sm_test";

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
		Files.writeString(raftXmlFile, raftXmlString());
		var nodeNames = new ArrayList<String>();
		for (int i = 0; i < ports.length; i++) {
			var raftConf = RaftConfig.loadFromString(raftXmlString());
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
		waitStableLeader();
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
	public void testFailedEditRollsBackLocalRegisters() throws Exception {
		var config = Config.load();
		config.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		config.getServiceManagerConf().setSessionName("UnitTest.FND5_31.Agent");
		var agent = new ServiceManagerAgentWithRaft(config);
		try {
			agent.start();
			agent.waitReady();

			var serviceA = new BServiceInfo("Fnd5EditRollback.A", "31", 0, "127.0.0.1", 1231);
			agent.registerService(serviceA);
			Assertions.assertTrue(registersOf(agent).containsKey(serviceA), "前置：注册成功");

			// 停掉两个follower：leader失去多数派，Edit必然无法提交（超时/错误码）。
			var leaderIdx = -1;
			for (int i = 0; i < rocksList.size(); i++)
				if (rocksList.get(i).isLeader())
					leaderIdx = i;
			Assertions.assertTrue(leaderIdx >= 0, "应有leader");
			var stopped = new ArrayList<Integer>();
			for (int i = 0; i < servers.size(); i++)
				if (i != leaderIdx) {
					rocksList.get(i).getRaft().getServer().stop();
					stopped.add(i);
				}
			try {
				// 失败注销：抛错且本地回滚——条目必须仍在（修复前本地已删，僵尸注册）。
				var serviceB = new BServiceInfo("Fnd5EditRollback.B", "32", 0, "127.0.0.1", 1232);
				Assertions.assertThrows(Exception.class, () -> agent.unRegisterService(serviceA),
						"失去多数派下注销必须失败");
				Assertions.assertTrue(registersOf(agent).containsKey(serviceA),
						"注销失败必须回滚本地registers（FND5-31）");

				// 失败注册：抛错且本地不落——修复前本地残留，重放会向服务端永远重放该僵尸注册。
				Assertions.assertThrows(Exception.class, () -> agent.registerService(serviceB),
						"失去多数派下注册必须失败");
				Assertions.assertFalse(registersOf(agent).containsKey(serviceB),
						"注册失败不得残留本地registers（FND5-31）");
			} finally {
				// 恢复多数派：closeSession清理与后续重试才能收敛。
				for (var i : stopped)
					rocksList.get(i).getRaft().getServer().start();
			}
		} finally {
			// 先关agent触发服务端closeSession（多数派已恢复），定界等待会话清理收敛后再由
			// @AfterAll拆集群——曾在此直接teardown，与closeSession退避重试竞争Service锁，
			// 全量并行时序下@AfterAll（无超时保护）永久挂起拖死整个suite。
			agent.close();
			waitSessionCleaned();
		}
	}

	/** 定界等待（≤60s）三个节点的tSession均无本测试会话行；超时仅记录，teardown由@Timeout兜底。 */
	private static void waitSessionCleaned() throws Exception {
		var sessionName = "UnitTest.FND5_31.Agent";
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
		System.err.println("FND5-31 test: session row not cleaned in 60s, teardown proceeds with @Timeout guard.");
	}

	@SuppressWarnings("unchecked")
	private static Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession> sessionTableOf(
			ServiceManagerWithRaft server) throws Exception {
		Field f = ServiceManagerWithRaft.class.getDeclaredField("tableSession");
		f.setAccessible(true);
		return (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession>)f.get(server);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<BServiceInfo, BServiceInfo> registersOf(ServiceManagerAgentWithRaft agent)
			throws Exception {
		Field f = ServiceManagerAgentWithRaft.class.getDeclaredField("registers");
		f.setAccessible(true);
		return (ConcurrentHashMap<BServiceInfo, BServiceInfo>)f.get(agent);
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

	private static void waitStableLeader() throws Exception {
		long deadline = System.currentTimeMillis() + 90_000;
		long stableSince = 0;
		int stableIdx = -1;
		while (System.currentTimeMillis() < deadline) {
			int leaderIdx = -1;
			for (int i = 0; i < rocksList.size(); i++)
				if (rocksList.get(i).isLeader())
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
