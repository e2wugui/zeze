package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Selectors;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Services.GlobalCacheManagerWithRaft;
import Zeze.Services.HandshakeClient;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND3-35：raft GCM release把最后一个持有者清掉后置 transient 的 acquireStatePending
 * =StateRemoved 并remove记录，随后过程返回、提交走raft共识。_final_commit_失败
 * （appendLog超时/重试）回滚时：持有者被事务日志复原（modify/share字段只在apply时改，
 * 回滚即弃日志），但StateRemoved无日志、不会自动复位——回滚后缓存里同一个bean的占位
 * 永久停留StateRemoved，该key上所有后续acquire/release进入无await的continue忙自旋
 * （100% CPU、pLock永久持有），globalLruTryRemove只放行Invalid，毒化bean永不逐出，
 * daemon的walkKey碰到冻结key会停摆整个守护。仅重启可恢复。
 * <p>
 * 修复：事务层_final_rollback_在"过程成功返回但_final_commit_失败"路径同样执行已注册
 * 的回滚动作（此前lastRollbackActions只由rollback()赋值，该路径恒为null）；GCM四处
 * transient占位统一用runWhileRollback注册复位。
 * <p>
 * 构造性场景（3节点进程内raft GCM，时序全部由测试控制）：
 * 1. Peer A 登录leader并真实获取 KEY 的Modify（走raft提交）；
 * 2. 关闭两个follower（集群失去quorum，本实现无quorum主动退位，leader保持isLeader
 *    但appendLog永远等不到多数确认）；
 * 3. release(A, KEY)：过程成功返回，_final_commit_的appendLog按
 *    2*AppendEntriesTimeout+1000 超时抛RaftRetryException走回滚；
 * 4. 断言占位复位为Invalid（修复前永久停留StateRemoved）、记录与持有者被复原
 *    （回滚正确性的直接证据）；
 * 5. 再次release必须能完成而不是忙自旋（修复前第二个调用即在此冻结）。
 */
@Fast
public class TestGlobalCacheManagerRaftReleaseRemovedReset {
	private static final Binary KEY = new Binary("UnitTest.FND3_35.Key".getBytes(StandardCharsets.UTF_8));
	private static final Binary WARMUP_KEY = new Binary("UnitTest.FND3_35.WarmUp".getBytes(StandardCharsets.UTF_8));
	private static final int SERVER_ID_A = 9301;
	private static final String RAFT_NAME = "fnd3_35_gcm_test";
	// AppendEntriesTimeout最小值1000（RaftConfig.verify），LeaderHeartbeatTimer须≥其+200。
	// 缩短后：每次提交失败的appendLog等待=2*1000+1000ms，加命运判定同额，单次release约6~7s。
	private static final int APPEND_ENTRIES_TIMEOUT = 1000;
	private static final int LEADER_HEARTBEAT_TIMER = 1200;

	// 3节点raft（Raft构造强制>=3），随机端口避免并行测试冲突；DbHome由Raft按节点名派生，收尾按实际值清理。
	private static final int[] ports = new int[3];
	private static final ArrayList<GlobalCacheManagerWithRaft> nodes = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static GlobalCacheManagerWithRaft gcm; // leader
	private static int leaderIndex;
	private static Path raftXmlFile;
	private static Peer clientA;
	private static Table<Binary, BCacheState> globalStates; // 反射取得，只读
	private static final AtomicLong requestIds = new AtomicLong();
	private static final boolean[] closed = new boolean[3];

	/** 裸协议raft客户端：Login/Acquire正常收发。 */
	private static final class Peer extends HandshakeClient {
		Peer(String name) {
			super(name, new Zeze.Config());
			// LeaderIs：raft服务端会向所有已握手的连接推送（重定向/通告），必须注册才能解码，
			// 否则连接被UnknownProtocol关闭（对齐Raft.Agent.NetClient的注册）。
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Critical));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
		}

		AsyncSocket connect(int port) throws Exception {
			// WaitReady()固定5s超时：全量负载下TCP连接+握手可能超时（TimeoutException，
			// 实测偶发），有界重试：失败连接remove+stop后重建（连接建立幂等）。
			for (int attempt = 1; ; ++attempt) {
				var connector = new Connector("127.0.0.1", port, false);
				getConfig().addConnector(connector);
				start();
				try {
					return connector.WaitReady();
				} catch (Exception e) { // 超时经Task.forceThrow sneaky-throw受检TimeoutException，编译期不可见
					if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
						throw e;
					getConfig().removeConnector(connector);
					connector.stop();
					//noinspection BusyWait
					Thread.sleep(200);
				}
			}
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		// 进程内3节点raft+客户端共享Selectors，默认1个selector线程不够用（参见
		// GlobalCacheManagerAsyncServer.main的相同处理）
		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (Selectors.getInstance().getCount() < cpuCount)
			Selectors.getInstance().add(cpuCount - Selectors.getInstance().getCount());
		// freePort存在TOCTOU：并行负载下可能返回重复端口，RaftConfig.addNode的重复节点校验
		// 会直接initializationError（20轮压测1次duplicate node）。取值去重，重复即重取。
		var usedPorts = new java.util.HashSet<Integer>();
		// Windows下启动前清理，保证全新状态；收尾best-effort。dbHome由节点名(Host_Port)派生，
		// freePort跨测试/跨轮复用端口号时，Raft会打开含异构raft日志的残留目录（收尾deleteDirectory
		// 对RocksDB延迟释放的句柄会静默失败而累积），leader重发/apply时decode撞unknown table
		// template，集群永久卡死（90s leader未ready / setUp 300s超时）。同族先例：
		// TestServiceManagerWithRaftSuspect.cleanDirs。
		for (int port : ports) {
			LogSequence.deleteDirectory(new File("127.0.0.1_" + port));
			LogSequence.deleteDirectory(new File(RAFT_NAME + "_127.0.0.1_" + port));
		}
		for (int i = 0; i < ports.length; i++) {
			int p = freePort();
			while (!usedPorts.add(p))
				p = freePort();
			ports[i] = p;
		}
		raftXmlFile = Files.createTempFile(RAFT_NAME, ".xml");
		Files.writeString(raftXmlFile, raftXmlString());
		var nodeNames = new ArrayList<String>();
		for (int i = 0; i < ports.length; i++) {
			// 每个节点独立的RaftConfig（Raft构造会改写传入配置的Name/DbHome，不能共享）
			var raftConf = RaftConfig.loadFromString(raftXmlString());
			for (var node : raftConf.getNodes().values())
				if (node.getPort() == ports[i])
					nodeNames.add(node.getName());
			nodes.add(new GlobalCacheManagerWithRaft(nodeNames.get(i), raftConf, new Zeze.Config(), false));
			dbHomes.add(raftConf.getDbHome());
		}
		waitLeader();
		Assertions.assertNotNull(gcm, "3节点集群必须选出leader");

		var globalStatesField = GlobalCacheManagerWithRaft.class.getDeclaredField("globalStates");
		globalStatesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var table = (Table<Binary, BCacheState>)globalStatesField.get(gcm);
		globalStates = table;
		ensureLeaderReady();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (clientA != null)
			clientA.stop();
		for (int i = 0; i < nodes.size(); i++) {
			// 测试中途已关闭的follower（失quorum构造）不重复close
			if (!closed[i])
				nodes.get(i).close();
		}
		nodes.clear();
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
				sb.append("\t<node Host=\"127.0.0.1\" Port=\"").append(port)
						.append("\" AppendEntriesTimeout=\"").append(APPEND_ENTRIES_TIMEOUT)
						.append("\" LeaderHeartbeatTimer=\"").append(LEADER_HEARTBEAT_TIMER)
						.append("\"/>\n");
		return sb.append("</raft>\n").toString();
	}

	private static int freePort() throws Exception {
		try (var socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	/** 同一节点连续isLeader()满5秒视为稳定：本机默认定时器下选举初期会抖动十余秒再收敛。 */
	private static void waitLeader() throws InterruptedException {
		long deadline = System.currentTimeMillis() + 90_000;
		long stableSince = 0;
		int stableIdx = -1;
		while (System.currentTimeMillis() < deadline) {
			int leader = -1;
			for (int i = 0; i < nodes.size(); i++)
				if (nodes.get(i).getRocks().isLeader())
					leader = i;
			long now = System.currentTimeMillis();
			if (leader < 0) {
				stableIdx = -1;
			} else if (leader != stableIdx) {
				stableIdx = leader;
				stableSince = now;
			} else if (now - stableSince >= 5_000) {
				gcm = nodes.get(leader);
				leaderIndex = leader;
				return;
			}
			//noinspection BusyWait
			Thread.sleep(100);
		}
		Assertions.fail("90s内未出现稳定leader");
	}

	/**
	 * 静默集群上，选举时写入的SetLeaderReadyEvent可能因初始发送被跳过（socket未ready/stale pending）
	 * 而搁置：leader保持当选但never-ready，waitLeaderReady()永久阻塞所有wire请求（Login/Acquire）。
	 * 用一个raft写强制触发trySendAppendEntries全量重发，解开搁置并等待ready。
	 */
	private static void ensureLeaderReady() throws InterruptedException {
		var raft = gcm.getRocks().getRaft();
		long deadline = System.currentTimeMillis() + 30_000;
		while (!raft.isReadyLeader()) {
			try {
				gcm.getRocks().newProcedure(() -> {
					globalStates.put(WARMUP_KEY, new BCacheState());
					return 0L;
				}).call();
			} catch (Throwable ex) {
				// 抖动期的RaftRetry/异常，重试
			}
			//noinspection BusyWait
			Thread.sleep(200);
		}
	}

	private static void sendLogin(AsyncSocket socket, int serverId) throws Exception {
		// RaftRetry(-15)：静默集群leader漂移/选举窗口的瞬态应答，有界重试（对齐SM-raft族
		// sendLogin），每次重试用新Login（requestId唯一）。
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			var login = new Login();
			login.Argument.setServerId(serverId);
			login.Argument.setGlobalCacheManagerHashIndex(0);
			login.getUnique().setRequestId(requestIds.incrementAndGet());
			login.setCreateTime(System.currentTimeMillis()); // 不设置会被服务端判为RaftExpired(-17)
			login.setTimeout(15_000);
			Assertions.assertTrue(login.SendForWait(socket, 15_000).await(15_000), "login await");
			Assertions.assertFalse(login.isTimeout(), "login timeout");
			lastCode = login.getResultCode();
			if (lastCode != -15) {
				Assertions.assertEquals(0, lastCode, "login resultCode");
				return;
			}
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("login持续RaftRetry(-15)，lastCode=" + lastCode);
	}

	/** A的Modify-Acquire发送并断言成功（RaftRetry(-15)有界重试，理由同sendLogin）。 */
	private static void sendAcquireModify(AsyncSocket socket) throws Exception {
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			var rpc = new Acquire();
			rpc.Argument.setGlobalKey(KEY);
			rpc.Argument.setState(GlobalCacheManagerConst.StateModify);
			rpc.getUnique().setRequestId(requestIds.incrementAndGet());
			rpc.setCreateTime(System.currentTimeMillis());
			rpc.setTimeout(15_000);
			Assertions.assertTrue(rpc.SendForWait(socket, 15_000).await(15_000), "A acquire await");
			Assertions.assertFalse(rpc.isTimeout(), "A acquire timeout");
			lastCode = rpc.getResultCode();
			if (lastCode != -15) {
				Assertions.assertEquals(0, lastCode, "A acquire resultCode");
				return;
			}
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("A acquire持续RaftRetry(-15)，lastCode=" + lastCode);
	}

	/** 表读取必须在事务内进行（Table.get需要Transaction.getCurrent()），用只读procedure包一层。 */
	private static int pendingOf(Binary key) throws Exception {
		var pending = new int[1];
		gcm.getRocks().newProcedure(() -> {
			var cs = globalStates.get(key);
			pending[0] = cs != null ? cs.getAcquireStatePending() : GlobalCacheManagerConst.StateInvalid;
			return 0L;
		}).call();
		return pending[0];
	}

	/** 记录持有者（-1表示无记录/无持有者），读取理由同pendingOf。 */
	private static int modifyOf(Binary key) throws Exception {
		var modify = new int[1];
		gcm.getRocks().newProcedure(() -> {
			var cs = globalStates.get(key);
			modify[0] = cs != null ? cs.getModify() : -1;
			return 0L;
		}).call();
		return modify[0];
	}

	private static Object sessionOf(int serverId) throws Exception {
		var sessionsField = GlobalCacheManagerWithRaft.class.getDeclaredField("sessions");
		sessionsField.setAccessible(true);
		var sessions = (LongConcurrentHashMap<?>)sessionsField.get(gcm);
		var holder = sessions.get(serverId);
		Assertions.assertNotNull(holder, "session must exist, serverId=" + serverId);
		return holder;
	}

	private static Method findReleaseMethod() {
		for (var m : GlobalCacheManagerWithRaft.class.getDeclaredMethods()) {
			if (m.getName().equals("release") && m.getParameterCount() == 2
					&& m.getParameterTypes()[1] == Binary.class)
				return m;
		}
		return null;
	}

	@Test
	@Timeout(180)
	public void testCommitFailMustResetRemovedPending() throws Throwable {

		// 1. A登录leader并真实获取KEY的Modify（走raft提交）
		clientA = new Peer("UnitTest.FND3_35.A");
		var socketA = clientA.connect(ports[leaderIndex]);
		sendLogin(socketA, SERVER_ID_A);
		sendAcquireModify(socketA);
		Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingOf(KEY), "A完成后申请位为Invalid");
		Assertions.assertEquals(SERVER_ID_A, modifyOf(KEY), "A是Modify持有者");

		// 2. 关闭两个follower：集群失去quorum。本实现无quorum主动退位，leader保持isLeader，
		//    但appendLog永远等不到多数派确认，按 2*AppendEntriesTimeout+1000 超时抛RaftRetry。
		for (int i = 0; i < nodes.size(); i++) {
			if (i != leaderIndex) {
				nodes.get(i).close();
				closed[i] = true;
			}
		}

		// 3. release(A, KEY)：过程成功返回，_final_commit_的appendLog超时失败走_final_rollback_。
		//    修复前：lastRollbackActions恒null（该路径不经rollback()），StateRemoved滞留。
		var holderA = sessionOf(SERVER_ID_A);
		final Method release = findReleaseMethod();
		Assertions.assertNotNull(release, "release(CacheHolder,Binary)方法必须存在");
		release.setAccessible(true);
		var firstDone = new CountDownLatch(1);
		final AtomicBoolean firstReturned = new AtomicBoolean(false);
		var releaser1 = new Thread(() -> {
			try {
				release.invoke(gcm, holderA, KEY);
				firstReturned.set(true);
			} catch (Throwable ex) {
				// 回滚路径返回RaftRetry结果码不抛异常；其他异常由后续断言暴露
			} finally {
				firstDone.countDown();
			}
		}, "UnitTest.FND3_35.Releaser1");
		releaser1.setDaemon(true);
		releaser1.start();
		// appendLog等待3s + 命运判定3s + 余量
		Assertions.assertTrue(firstDone.await(30, TimeUnit.SECONDS), "第一次release必须返回（含appendLog超时等待）");
		releaser1.join(5_000);
		Assertions.assertTrue(firstReturned.get(), "release不能以异常结束");

		// 4. 核心断言①：transient占位必须复位（修复前：永久停留StateRemoved——毒化bean）
		Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingOf(KEY),
				"提交失败回滚后StateRemoved必须被回滚动作复位");

		// 核心断言②：记录与持有者被事务回滚复原（"已删除"从未发生）
		Assertions.assertEquals(SERVER_ID_A, modifyOf(KEY), "回滚后A仍是Modify持有者（remove被撤销）");

		// 5. 核心断言③：再次release必须能完成而不是忙自旋（修复前：pending==StateRemoved，
		//    第二个调用在while(true)的continue上100%CPU冻结、pLock永久持有）
		var secondDone = new CountDownLatch(1);
		final AtomicBoolean secondReturned = new AtomicBoolean(false);
		var releaser2 = new Thread(() -> {
			try {
				release.invoke(gcm, holderA, KEY);
				secondReturned.set(true);
			} catch (Throwable ex) {
				// 同上
			} finally {
				secondDone.countDown();
			}
		}, "UnitTest.FND3_35.Releaser2");
		releaser2.setDaemon(true);
		releaser2.start();
		Assertions.assertTrue(secondDone.await(30, TimeUnit.SECONDS), "第二次release不能忙自旋（key冻结/守护停摆）");
		releaser2.join(5_000);
		Assertions.assertTrue(secondReturned.get(), "第二次release不能以异常结束");
		Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingOf(KEY),
				"第二次release提交失败后占位同样复位");
	}
}
