package UnitTest.Zeze.Services;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
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
 * FND8-64 回归：cleanupSessionRow 的 sendNotifies 在事务体内 appendLog 之前执行——
 * 断连清理事务 RaftRetry 时每试一次就向订阅者重发一批 Edit(remove)（提交前推送），
 * 终败则订阅者已删而 tServerState 残留（视图分叉）。
 * 修复后经 RocksRaft Transaction.runWhileCommit 提交后发送。
 * 场景：A注册/B订阅 → 持两个follower的Raft锁使quorum不可达（选举同样被锁阻塞，
 * 时序确定）→ 突然杀掉A的连接触发closeSession → 阻塞窗口内B不得收到remove
 * （修复前在事务体内即时发出）；解锁恢复后提交成功，B恰好收到一次remove。
 */
@Fast
public class TestFnd864SmRaftCleanupNotifyAfterCommit {
	private static final String RAFT_NAME = "fnd8_64_sm_a5";
	private static final String SERVICE = "Fnd864Cleanup.Svc";
	private static final String SESSION_A = "UnitTest.FND8_64.Reg";
	private static final String SESSION_B = "UnitTest.FND8_64.Sub";

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
			ports[i] = probePort(used);
		raftXmlFile = Files.createTempFile(RAFT_NAME, ".xml");
		Files.writeString(raftXmlFile, raftXmlString());
		var nodeNames = new ArrayList<String>();
		// Windows下启动前清理，保证全新状态；收尾best-effort。dbHome由节点名(Host_Port)派生，
		// freePort跨测试/跨轮复用端口号时，Raft会打开含异构raft日志的残留目录（收尾deleteDirectory
		// 对RocksDB延迟释放的句柄会静默失败而累积），leader重发/apply时decode撞unknown table
		// template，集群永久卡死（90s leader未ready / setUp 300s超时）。同族先例：
		// TestServiceManagerWithRaftSuspect.cleanDirs。
		for (int port : ports) {
			LogSequence.deleteDirectory(new java.io.File("127.0.0.1_" + port));
			LogSequence.deleteDirectory(new java.io.File(RAFT_NAME + "_127.0.0.1_" + port));
		}
		for (int i = 0; i < ports.length; i++) {
			var raftConf = RaftConfig.loadFromString(raftXmlString());
			for (var node : raftConf.getNodes().values())
				if (node.getPort() == ports[i])
					nodeNames.add(node.getName());
			servers.add(new ServiceManagerWithRaft(nodeNames.get(i), raftConf, new Config(), false));
			// FND8-42起Raft构造器经derive私有副本联动DbHome，不再改写传入的raftConf——
			// raftConf.getDbHome()仍是xml Name而非数据目录，收尾删它是空操作，节点目录
			// 127.0.0.1_<port>因此残留。实际目录由节点名派生（derive口径，同上方预清理）。
			dbHomes.add(nodeNames.get(i).replace(':', '_'));
		}
		var rocksField = ServiceManagerWithRaft.class.getDeclaredField("rocks");
		rocksField.setAccessible(true);
		// 测试钩子接管fatalKill（否则halt会杀死测试JVM）：记录触发点，由断言暴露
		var hookMethod = Zeze.Raft.Raft.class.getDeclaredMethod("setFatalKillHookForTest", Runnable.class);
		hookMethod.setAccessible(true);
		for (var server : servers) {
			var raft = ((Rocks)rocksField.get(server)).getRaft();
			hookMethod.invoke(raft, (Runnable)() -> {
				//noinspection CallToPrintStackTrace
				new Throwable("fatalKill captured on " + raft.getName()).printStackTrace();
				fatalKills.incrementAndGet();
			});
			rocksList.add((Rocks)rocksField.get(server));
		}
		waitStableLeader();
	}

	private static final java.util.concurrent.atomic.AtomicInteger fatalKills = new java.util.concurrent.atomic.AtomicInteger();

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

	/** a5专属端口段探测（去重：同一raft配置内出现duplicate node会装配失败）。 */
	private static int probePort(java.util.HashSet<Integer> used) throws IOException {
		for (int port = 28640; port < 28699; port++) {
			if (!used.contains(port)) {
				try (var ignore = new ServerSocket(port)) {
					if (used.add(port))
						return port;
				} catch (IOException e) {
					// 端口被占，探测下一个
				}
			}
		}
		throw new IOException("no free port in [28640,28699)");
	}

	@Test
	@Timeout(300)
	public void testCleanupNotifySentOnlyAfterCommit() throws Exception {
		var removes = new CopyOnWriteArrayList<String>();
		var adds = new CopyOnWriteArrayList<String>();

		var configA = Config.load();
		configA.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		configA.getServiceManagerConf().setSessionName(SESSION_A);
		var agentA = new ServiceManagerAgentWithRaft(configA);
		var configB = Config.load();
		configB.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		configB.getServiceManagerConf().setSessionName(SESSION_B);
		var agentB = new ServiceManagerAgentWithRaft(configB);
		try {
			// A注册、B订阅并收到初始快照（add）
			agentA.start();
			agentA.waitReady();
			agentA.registerService(new BServiceInfo(SERVICE, "64", 0, "127.0.0.1", 1264));

			agentB.setOnChanged(edit -> {
				for (var e : edit.getRemove())
					removes.add(e.getServiceName());
				for (var e : edit.getAdd())
					adds.add(e.getServiceName());
			});
			agentB.start();
			agentB.waitReady();
			agentB.subscribeService(new BSubscribeInfo(SERVICE, 0));
			for (int i = 0; i < 300 && !adds.contains(SERVICE); i++)
				Thread.sleep(100);
			Assertions.assertTrue(adds.contains(SERVICE), "前置：订阅者收到初始快照add");

			// quorum不可达：停掉两个follower（FND5-31判例手法，比锁控温和——锁控会令
			// follower积压Apply触发divergence fatalKill）。leader保持（无更高term），
			// 清理事务每次appendLog等约5s（AppendEntriesTimeout*2+1s）后RaftRetry。
			int leaderIdx = -1;
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
				// 突然杀掉A到leader的连接（raft客户端与各节点均有连接，须按远端端口定位）：
				// 服务端OnSocketClose→closeSession→清理事务必然RaftRetry。
				// 修复前：sendNotifies在事务体内appendLog之前即时发出（首次尝试即达订阅者）。
				var raftClientField = ServiceManagerAgentWithRaft.class.getDeclaredField("raftClient");
				raftClientField.setAccessible(true);
				var netClient = ((Zeze.Raft.Agent)raftClientField.get(agentA)).getClient();
				var socketMapField = Zeze.Net.Service.class.getDeclaredField("socketMap");
				socketMapField.setAccessible(true);
				@SuppressWarnings("unchecked")
				var socketMap = (Zeze.Util.LongConcurrentHashMap<Zeze.Net.AsyncSocket>)socketMapField.get(netClient);
				var leaderPort = ports[leaderIdx];
				var killed = new int[]{0};
				for (var it = socketMap.entryIterator(); it.moveToNext(); ) {
					var so = it.value();
					if (so.getRemoteAddress() instanceof java.net.InetSocketAddress rsa && rsa.getPort() == leaderPort) {
						so.close(new Exception("fnd8-64 test kill"));
						killed[0]++;
					}
				}
				Assertions.assertEquals(1, killed[0], "应恰好杀掉A到leader的一条连接");
				// 立即关闭A的整个客户端（阻止自动重连+重Login重建会话行——否则重登录的
				// Login与清理重试在恢复后竞争提交，清理可能命中\"已被新连接接管\"守卫而跳过）
				agentA.close();

				// 阻塞窗口（15s，覆盖至少两次失败的清理尝试；修复前的首次事务体内即时发送
				// 在1s内即达订阅者）内不得收到remove：修复后经runWhileCommit注册，回滚不触发。
				for (int i = 0; i < 150; i++) {
					Assertions.assertTrue(removes.stream().noneMatch(SERVICE::equals),
						"quorum不可达时清理事务未提交，remove通知不得发送（提交前发送=FND8-64）");
					Thread.sleep(100);
				}
			} finally {
				// 恢复多数派：清理重试才能收敛。
				for (var i : stopped)
					rocksList.get(i).getRaft().getServer().start();
			}

			// 恢复多数派：清理事务重试提交成功后，remove经commit actions送达订阅者。
			// 注：raft存在既有"假失败"窗口（appendLog等待恰在quorum恢复边界超时，而日志
			// 条目随后仍被提交——LogSequence判例自认）——该次尝试的commit actions被丢弃、
			// 后续重试因行已清而空转，通知丢失属框架既有语义（对齐ProcessEditRequest应答
			// 的同窗口），订阅者由重连全量快照收敛。送达与未送达两种终态都接受，但都不得
			// 出现"提交前送达"（上方窗口断言已覆盖）或重复送达。
			for (int i = 0; i < 600 && removes.stream().filter(SERVICE::equals).count() < 1; i++)
				Thread.sleep(100);
			if (removes.stream().anyMatch(SERVICE::equals)) {
				for (int i = 0; i < 50; i++) // 收敛窗口：不得重复推送
					Thread.sleep(100);
				Assertions.assertEquals(1, removes.stream().filter(SERVICE::equals).count(), "不得重复推送remove");
			}
			// 硬断言：清理事务最终真实提交（会话行被清）——通知至多迟到（假失败窗口），
			// 但服务端状态必须收敛。
			Assertions.assertTrue(waitSessionRowCleaned(90_000), "清理事务必须最终提交（A的会话行被清）");
			Assertions.assertEquals(0, fatalKills.get(), "测试期间集群不得触发fatalKill");
		} finally {
			try {
				agentB.close();
			} catch (Throwable ignored) {
			}
			try {
				agentA.close();
			} catch (Throwable ignored) {
			}
			waitSessionCleaned();
		}
	}

	private static Field rocksFieldStatic() throws NoSuchFieldException {
		var f = ServiceManagerWithRaft.class.getDeclaredField("rocks");
		f.setAccessible(true);
		return f;
	}

	/** 定界等待（≤timeoutMs）三个节点的tSession均无A的会话行（清理事务已提交的硬证据）。 */
	private static boolean waitSessionRowCleaned(long timeoutMs) throws Exception {
		var f = ServiceManagerWithRaft.class.getDeclaredField("tableSession");
		f.setAccessible(true);
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			var found = new boolean[]{false};
			for (var server : servers) {
				try {
					@SuppressWarnings("unchecked")
					var table = (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession>)f.get(server);
					table.walk((name, v) -> {
						if (SESSION_A.equals(name))
							found[0] = true;
						return true;
					});
				} catch (Throwable ignored) {
					// 节点抖动窗口walk失败：继续轮询
				}
			}
			if (!found[0])
				return true;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		return false;
	}

	/** 定界等待（≤60s）三个节点的tSession均无本测试会话行；超时仅记录，teardown由@Timeout兜底。 */
	private static void waitSessionCleaned() throws Exception {
		List<String> sessions = List.of(SESSION_A, SESSION_B);
		long deadline = System.currentTimeMillis() + 60_000;
		var f = ServiceManagerWithRaft.class.getDeclaredField("tableSession");
		f.setAccessible(true);
		while (System.currentTimeMillis() < deadline) {
			var found = new boolean[]{false};
			for (var server : servers) {
				try {
					@SuppressWarnings("unchecked")
					var table = (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BSession>)f.get(server);
					table.walk((name, v) -> {
						if (sessions.contains(name))
							found[0] = true;
						return true;
					});
				} catch (Throwable ignored) {
					// 节点抖动窗口walk失败：继续轮询
				}
			}
			if (!found[0])
				return;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		System.err.println("FND8-64 test: session rows not cleaned in 60s, teardown proceeds with @Timeout guard.");
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
			if (leaderIdx < 0) {
				stableIdx = -1;
			} else if (leaderIdx != stableIdx) {
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
