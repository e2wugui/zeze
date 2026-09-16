package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Net.AsyncSocket;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND4-57 红绿回归：断连清理事务的 RaftRetry 返回码必须闭环（重试+对账）。
 * 曾经 procedure.call() 返回码被忽略：失去多数派时 OnSocketClose 的清理回滚后
 * 不重试不告警，死会话行永久残留 raft 表。
 * 场景：Login 建行 → 停掉两个 follower（失去多数派）→ 断开客户端 socket 触发
 * OnSocketClose → 清理 appendLog 必然 RaftRetry → 重启 follower 恢复多数派 →
 * 断言 tSession 行最终被清理（退避重试或 60s 对账收敛，两者都是本次修复的产物；
 * 修复前两者皆无，行永久残留——红）。
 * 基建对齐 TestServiceManagerWithRaftAllocateId（3节点进程内raft+裸协议Peer）。
 */
@Fast
public class TestServiceManagerWithRaftSessionCloseRetry {
	private static final String RAFT_NAME = "fnd4_57_sm_test";
	private static final String SESSION_NAME = "UnitTest.FND4_57.Agent";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;
	private static Peer client;

	private static final class Peer extends HandshakeClient {
		Peer(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect(int port) throws Exception {
			// 类并行高负载下WaitReady的5s预算偶发不够（目标是本地监听端口、必然可达，纯墙钟问题，
			// 50轮压测~13%假红）：整体重建connector重试，共3次、预算15s。
			Exception last = null;
			for (int attempt = 0; attempt < 3; attempt++) {
				var connector = new Zeze.Net.Connector("127.0.0.1", port, false);
				getConfig().addConnector(connector);
				if (attempt == 0)
					start(); // 首个connector由服务start驱动；服务已启动后加入的connector需自行start
				else
					connector.start();
				try {
					return connector.WaitReady();
				} catch (Exception e) {
					last = e;
					getConfig().removeConnector(connector);
					connector.stop();
				}
			}
			throw last;
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (Zeze.Net.Selectors.getInstance().getCount() < cpuCount)
			Zeze.Net.Selectors.getInstance().add(cpuCount - Zeze.Net.Selectors.getInstance().getCount());
		for (int i = 0; i < ports.length; i++)
			ports[i] = freePort();
		raftXmlFile = Files.createTempFile(RAFT_NAME, ".xml");
		Files.writeString(raftXmlFile, raftXmlString());
		var nodeNames = new ArrayList<String>();
		for (int i = 0; i < ports.length; i++) {
			var raftConf = RaftConfig.loadFromString(raftXmlString());
			for (var node : raftConf.getNodes().values())
				if (node.getPort() == ports[i])
					nodeNames.add(node.getName());
			servers.add(new ServiceManagerWithRaft(nodeNames.get(i), raftConf, new Zeze.Config(), false));
			dbHomes.add(raftConf.getDbHome());
		}
		fillRocksList();
		waitStableLeader();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (client != null)
			client.stop();
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
			LogSequence.deleteDirectory(new File(dbHome));
		dbHomes.clear();
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

	private static final java.util.HashSet<Integer> usedPorts = new java.util.HashSet<>();

	private static int freePort() throws Exception {
		while (true) {
			try (var socket = new ServerSocket(0)) {
				if (usedPorts.add(socket.getLocalPort()))
					return socket.getLocalPort();
			}
		}
	}

	private static void fillRocksList() throws Exception {
		var rocksField = ServiceManagerWithRaft.class.getDeclaredField("rocks");
		rocksField.setAccessible(true);
		for (var server : servers)
			rocksList.add((Rocks)rocksField.get(server));
	}

	@SuppressWarnings("unchecked")
	private static Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession> sessionTable(Rocks rocks)
			throws Exception {
		var field = ServiceManagerWithRaft.class.getDeclaredField("tableSession");
		field.setAccessible(true);
		var server = servers.get(rocksList.indexOf(rocks));
		return (Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession>)field.get(server);
	}

	private static Rocks leaderRocks() {
		for (var rocks : rocksList)
			if (rocks.isLeader())
				return rocks;
		return null;
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

	private static int leaderPort() {
		for (int i = 0; i < rocksList.size(); i++)
			if (rocksList.get(i).isLeader())
				return ports[i];
		throw new IllegalStateException("no leader");
	}

	private static void login(AsyncSocket sock, String sessionName) throws Exception {
		var login = new Login();
		login.Argument.setSessionName(sessionName);
		login.getUnique().setRequestId(System.nanoTime());
		login.setCreateTime(System.currentTimeMillis());
		login.setTimeout(30_000);
		Assertions.assertTrue(login.SendForWait(sock, 30_000).await(30_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
	}

	private static boolean sessionRowExists() throws Exception {
		// walk走存储层快照（raft已提交状态），不需要事务上下文；查leader（可能换届，重试）。
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			var rocks = leaderRocks();
			if (rocks != null) {
				var found = new boolean[1];
				sessionTable(rocks).walk((name, v) -> {
					if (SESSION_NAME.equals(name))
						found[0] = true;
					return true;
				});
				return found[0];
			}
			//noinspection BusyWait
			Thread.sleep(100);
		}
		throw new IllegalStateException("10s内无leader可查");
	}

	@Test
	@Timeout(200)
	public void testSessionCloseRetryOrReconcile() throws Exception {
		client = new Peer("UnitTest.FND4_57.Client");
		var leaderPort = leaderPort();
		var leaderIdx = -1;
		for (int i = 0; i < ports.length; i++)
			if (ports[i] == leaderPort)
				leaderIdx = i;
		var socket = client.connect(leaderPort);
		login(socket, SESSION_NAME);
		Assertions.assertTrue(sessionRowExists(), "前置：Login后tSession有行");

		// 停掉两个follower：leader失去多数派，后续appendLog必然RaftRetry。
		var stopped = new ArrayList<Integer>();
		for (int i = 0; i < servers.size(); i++) {
			if (i != leaderIdx) {
				rocksList.get(i).getRaft().getServer().stop();
				stopped.add(i);
			}
		}

		// 断开客户端socket：leader上触发OnSocketClose，清理事务RaftRetry回滚。
		socket.close(new java.io.IOException("test disconnect"));

		// 留出首次失败+若干退避重试的窗口（都失败：多数派未恢复）。
		//noinspection BusyWait
		Thread.sleep(2_000);

		// 恢复多数派：重启followers，退避重试（closeSession）或60s对账开始收敛。
		// 类并行下2s停机窗口内原端口可能被其他测试的freePort探测/临时绑定占用（第七轮压测×5、
		// 本地复现的BindException）；Acceptor.Start在bind失败时socket保持null可安全重试，
		// 有界重试等瞬时占用释放，永久被占则重抛。
		for (var i : stopped) {
			for (int attempt = 1; ; ++attempt) {
				try {
					rocksList.get(i).getRaft().getServer().start();
					break;
				} catch (IllegalStateException e) { // TcpSocket构造包装BindException
					if (attempt >= 20)
						throw e;
					//noinspection BusyWait
					Thread.sleep(500);
				}
			}
		}

		// 等待收敛：退避重试最长约13s，对账周期60s——上限给120s。
		long deadline = System.currentTimeMillis() + 120_000;
		while (System.currentTimeMillis() < deadline) {
			if (!sessionRowExists())
				return; // 清理落地（重试或对账）
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("会话行在恢复多数派后120s内未被清理（清理返回码未闭环或对账未生效）");
	}
}
