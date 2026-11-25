package Zeze.Services;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Util.Task;
import harness.Fast;
import harness.ForkedJvm;

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
@ForkedJvm
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestServiceManagerWithRaftSuspect {

	private static final String raftXmlString = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="s4_sm_test">
				<node Host="127.0.0.1" Port="19511"/>
				<node Host="127.0.0.1" Port="19512"/>
				<node Host="127.0.0.1" Port="19513"/>
			</raft>
			""";
	private static @Nullable Path raftXmlFile;
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		cleanDirs();
		raftXmlFile = Files.createTempFile("s4_sm_test_raft", ".xml");
		Files.writeString(raftXmlFile, raftXmlString);
			for (var node : RaftConfig.loadFromString(raftXmlString).getNodes().values())
				servers.add(new ServiceManagerWithRaft(node.getName(),
						RaftConfig.loadFromString(raftXmlString), new Config()));
	}

	@AfterAll
	public static void tearDown() throws Exception {
		for (var sm : servers)
			sm.close();
		servers.clear();
		Files.deleteIfExists(requireNonNull(raftXmlFile));
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
		requireNonNull(config.getServiceManagerConf()).setRaftXml(requireNonNull(raftXmlFile).toString());
		config.getServiceManagerConf().setSessionName(sessionName);
		var agent = new ServiceManagerAgentWithRaft(config);
		agent.start();
		agent.waitReady();
		return agent;
	}

	@Test
	@Order(1)
	@Timeout(120)
	public void testSuspectBroadcastOnDisconnect() throws Exception {
		var suspects = new ConcurrentLinkedQueue<Integer>();
		var watcher = newAgent("UnitTest.S4.Watcher", 2);
		try {
			watcher.setOnSuspect(suspects::add);

			var dead = newAgent("UnitTest.S4.Dead", 1);
			try {
				// 确定性等Identify到达服务端：onLoginSuccess先发Identify（同连接线上序在前），
				// raft请求通道FIFO——本空edit的应答到达即Identify必然已被处理（裸sleep(1000)
				// 是假同步点：断线RST掐掉在途Identify则SM不广播Suspect，压测下假红，
				// 对齐TestTakeoverIdentifySuspect判例）。
				dead.editService(new Zeze.Services.ServiceManager.BEditService());
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

	@Test
	@Order(2)
	@Timeout(120)
	public void testCloseCancelsSuspectTiersAndSuppressesLateStart() throws Exception {
		var watcherA = newAgent("UnitTest.S4.CloseA", 11);
		var watcherB = newAgent("UnitTest.S4.CloseB", 12);
		var dead = newAgent("UnitTest.S4.CloseDead", 13);
		try {
			watcherA.editService(new Zeze.Services.ServiceManager.BEditService());
			watcherB.editService(new Zeze.Services.ServiceManager.BEditService());
			dead.editService(new Zeze.Services.ServiceManager.BEditService());

			var preShutdownSuspect = new LinkedBlockingQueue<Integer>();
			watcherA.setOnSuspect(preShutdownSuspect::add);

			// 断开 dead 在 leader 上创建一条活跃的 900s 升档定时器
			dead.close();
			Assertions.assertEquals(13, preShutdownSuspect.poll(15, TimeUnit.SECONDS),
					"disconnecting dead agent must create suspect tier on leader");

			var tiersField = ServiceManagerWithRaft.class.getDeclaredField("suspectTiers");
			tiersField.setAccessible(true);
			ServiceManagerWithRaft leader = null;
			ConcurrentHashMap<Integer, Object> leaderTiers = null;
			for (var sm : servers) {
				@SuppressWarnings("unchecked")
				var tiers = (ConcurrentHashMap<Integer, Object>)requireNonNull(tiersField.get(sm));
				if (tiers.containsKey(13)) {
					leader = sm;
					leaderTiers = tiers;
					break;
				}
			}
			Assertions.assertNotNull(leader, "leader must hold active suspect tier for dead serverId=13");
			var existingTier = requireNonNull(requireNonNull(leaderTiers).get(13));
			var timerField = existingTier.getClass().getDeclaredField("timer");
			timerField.setAccessible(true);
			var existingTimer = (Future<?>)requireNonNull(timerField.get(existingTier));
			Assertions.assertFalse(existingTimer.isCancelled());

			var shutdownSuspects = new ConcurrentLinkedQueue<Integer>();
			watcherA.setOnSuspect(shutdownSuspects::add);
			watcherB.setOnSuspect(shutdownSuspects::add);

			// 先关 leader 再关其余节点，避免切主后对 watcherA/watcherB 误报
			var toClose = new ArrayList<ServiceManagerWithRaft>();
			toClose.add(leader);
			for (var sm : servers)
				if (sm != leader)
					toClose.add(sm);
			servers.clear();
			for (var sm : toClose)
				sm.close();
			Thread.sleep(200);

			Assertions.assertTrue(existingTimer.isCancelled(),
					"existing suspect tier timer must be cancelled on close");
			Assertions.assertTrue(leaderTiers.isEmpty(),
					"suspectTiers must be empty after close");
			Assertions.assertTrue(shutdownSuspects.isEmpty(),
					"shutdown must not broadcast Suspect for closing clients, got=" + shutdownSuspects);

			// 停机后即便有迟到调用进入 startSuspectTier 也必须被 closed 闸门拦截，不得再插入定时器
			var startSuspectTier = ServiceManagerWithRaft.class.getDeclaredMethod(
					"startSuspectTier", int.class, long.class, long.class);
			startSuspectTier.setAccessible(true);
			startSuspectTier.invoke(leader, 999, 1L, 0L);
			Assertions.assertTrue(leaderTiers.isEmpty(),
					"closed ServiceManagerWithRaft must reject late startSuspectTier");
		} finally {
			dead.close();
			watcherA.close();
			watcherB.close();
		}
	}
}
