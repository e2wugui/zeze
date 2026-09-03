package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce;
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
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND-S1-3：raft GCM acquire把 transient 的 acquireStatePending 置位后，异常路径
 * （lockey.await上的中断、rocks IO异常等）无复位——Procedure回滚只撤销有日志的修改，
 * 共享bean的申请位永久停留Share/Modify：该key上后续acquire要么IllegalStateException
 * 要么永久park（procedure池线程泄漏），daemon的release也会永久await并停摆整个守护。
 * <p>
 * 构造性场景（3节点进程内raft GCM，时序全部由测试控制）：
 * 1. Peer A 登录leader并真实获取 KEY 的Modify（走raft提交）；
 * 2. Peer B 登录后，构造 B 的 Acquire(KEY, Share) 过程（直接驱动leader端私有acquireShare），
 *    B 占住申请位(StateShare)、向A发Reduce后park在 lockey.await()；
 * 3. 中断该过程线程（模拟任务超时看门狗/调度中断）——InterruptedException走Procedure回滚路径；
 * 4. 断言申请位复位为Invalid（修复前永久停留StateShare），且 release(A, KEY) 能正常完成
 * 而不是永久park（修复前daemon路径即在此冻结、守护停摆）。
 */
@Fast
public class TestGlobalCacheManagerRaftAcquirePendingReset {
	private static final Binary KEY = new Binary("UnitTest.FND_S1_3.Key".getBytes(StandardCharsets.UTF_8));
	private static final Binary WARMUP_KEY = new Binary("UnitTest.FND_S1_3.WarmUp".getBytes(StandardCharsets.UTF_8));
	private static final int SERVER_ID_A = 9201;
	private static final int SERVER_ID_B = 9202;
	private static final String RAFT_NAME = "s13_gcm_test";

	// 3节点raft（Raft构造强制>=3），随机端口避免并行测试冲突；DbHome由Raft按节点名派生，收尾按实际值清理。
	private static final int[] ports = new int[3];
	private static final ArrayList<GlobalCacheManagerWithRaft> nodes = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static GlobalCacheManagerWithRaft gcm; // leader
	private static int leaderPort;
	private static Path raftXmlFile;
	private static Peer clientA;
	private static Peer clientB;
	private static Table<Binary, BCacheState> globalStates; // 反射取得，只读
	private static final AtomicLong requestIds = new AtomicLong();

	/** 裸协议raft客户端：Login/Acquire正常收发；Reduce到达后park住由测试控制应答时机。 */
	private static final class Peer extends HandshakeClient {
		private final BlockingQueue<Reduce> reduces = new LinkedBlockingQueue<>();

		Peer(String name) {
			super(name, new Zeze.Config());
			// LeaderIs：raft服务端会向所有已握手的连接推送（重定向/通告），必须注册才能解码，
			// 否则连接被UnknownProtocol关闭（对齐Raft.Agent.NetClient的注册）。
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Reduce.TypeId_, new ProtocolFactoryHandle<>(
					Reduce::new, this::processReduce, TransactionLevel.None, DispatchMode.Direct));
		}

		private long processReduce(Reduce rpc) {
			reduces.add(rpc); // 不自动应答
			return 0;
		}

		AsyncSocket connect(int port) throws Exception {
			var connector = new Connector("127.0.0.1", port, false);
			getConfig().addConnector(connector);
			start();
			return connector.WaitReady();
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
		for (int i = 0; i < ports.length; i++)
			ports[i] = freePort();
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
		if (clientB != null)
			clientB.stop();
		for (var node : nodes)
			node.close();
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
			sb.append("\t<node Host=\"127.0.0.1\" Port=\"").append(port).append("\"/>\n");
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
				leaderPort = ports[leader];
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
		var login = new Login();
		login.Argument.setServerId(serverId);
		login.Argument.setGlobalCacheManagerHashIndex(0);
		login.getUnique().setRequestId(requestIds.incrementAndGet());
		login.setCreateTime(System.currentTimeMillis()); // 不设置会被服务端判为RaftExpired(-17)
		login.setTimeout(15_000);
		Assertions.assertTrue(login.SendForWait(socket, 15_000).await(15_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
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

	private static Object sessionOf(int serverId) throws Exception {
		var sessionsField = GlobalCacheManagerWithRaft.class.getDeclaredField("sessions");
		sessionsField.setAccessible(true);
		var sessions = (LongConcurrentHashMap<?>)sessionsField.get(gcm);
		var holder = sessions.get(serverId);
		Assertions.assertNotNull(holder, "session must exist, serverId=" + serverId);
		return holder;
	}

	/** 取某会话在leader端的socket（Login后绑定），用于构造直接驱动服务端过程的rpc。 */
	private static AsyncSocket serverSocketOf(int serverId) throws Exception {
		var holder = sessionOf(serverId);
		var sessionIdField = holder.getClass().getDeclaredField("sessionId");
		sessionIdField.setAccessible(true);
		long sessionId = sessionIdField.getLong(holder);
		var socket = gcm.getRocks().getRaft().getServer().GetSocket(sessionId);
		Assertions.assertNotNull(socket, "server socket of session " + serverId);
		return socket;
	}

	private static void waitPending(int expected, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (pendingOf(KEY) == expected)
				return;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		Assertions.assertEquals(expected, pendingOf(KEY), message);
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
	@Timeout(150)
	public void testAcquireExceptionMustResetPending() throws Throwable {

		// 1. A登录leader并真实获取KEY的Modify（走raft提交）
		clientA = new Peer("UnitTest.FND_S1_3.A");
		var socketA = clientA.connect(leaderPort);
		sendLogin(socketA, SERVER_ID_A);
		var acquireA = new Acquire();
		acquireA.Argument.setGlobalKey(KEY);
		acquireA.Argument.setState(GlobalCacheManagerConst.StateModify);
		acquireA.getUnique().setRequestId(requestIds.incrementAndGet());
		acquireA.setCreateTime(System.currentTimeMillis());
		acquireA.setTimeout(15_000);
		Assertions.assertTrue(acquireA.SendForWait(socketA, 15_000).await(15_000), "A acquire await");
		Assertions.assertFalse(acquireA.isTimeout(), "A acquire timeout");
		Assertions.assertEquals(0, acquireA.getResultCode(), "A acquire resultCode");
		Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingOf(KEY), "A完成后申请位为Invalid");

		// 2. B登录（仅建立会话，Acquire由测试直接驱动leader端过程）
		clientB = new Peer("UnitTest.FND_S1_3.B");
		var socketB = clientB.connect(leaderPort);
		sendLogin(socketB, SERVER_ID_B);
		var socketBOnServer = serverSocketOf(SERVER_ID_B);

		var rpcB = new Acquire();
		rpcB.Argument.setGlobalKey(KEY);
		rpcB.Argument.setState(GlobalCacheManagerConst.StateShare);
		rpcB.setSender(socketBOnServer); // sender.userState == B的CacheHolder

		var acquireShare = GlobalCacheManagerWithRaft.class.getDeclaredMethod("acquireShare", Acquire.class);
		acquireShare.setAccessible(true);
		var proc = gcm.getRocks().newProcedure(() -> {
			acquireShare.invoke(gcm, rpcB);
			return 0L;
		});

		// 3. 过程线程：占住申请位后向A发Reduce，然后park在lockey.await()
		var finished = new CountDownLatch(1);
		var worker = new Thread(() -> {
			try {
				proc.call(); // 期望被中断后走异常回滚，返回Procedure.Exception
			} catch (Throwable ex) {
				// procedure.call的异常已在内部处理；这里兜底记录
			} finally {
				finished.countDown();
			}
		}, "UnitTest.FND_S1_3.AcquireWorker");
		worker.setDaemon(true);
		worker.start();

		waitPending(GlobalCacheManagerConst.StateShare, 10_000, "B必须占住申请位");
		var reduce = clientA.reduces.poll(10, TimeUnit.SECONDS);
		Assertions.assertNotNull(reduce, "A必须收到Reduce");

		// 4. 中断过程线程（模拟任务超时看门狗/调度中断）——InterruptedException走Procedure回滚路径
		worker.interrupt();
		Assertions.assertTrue(finished.await(10, TimeUnit.SECONDS), "被中断的acquire过程必须返回");
		worker.join(10_000);

		// 5. 核心断言：transient申请位必须复位（修复前：回滚不撤销transient，永久停留StateShare）
		Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingOf(KEY),
				"acquire异常后申请位必须复位为Invalid");

		// 6. A应答挂起的Reduce（清理，避免遗留回调；BReduceParam编码要求globalKey非null）
		reduce.Result.setGlobalKey(reduce.Argument.getGlobalKey());
		reduce.Result.setState(GlobalCacheManagerConst.StateInvalid);
		reduce.SendResult();

		// 7. 功能断言：release(A, KEY)必须能完成而不是永久park（修复前daemon路径即在此冻结）
		var holderA = sessionOf(SERVER_ID_A);
		final Method release = findReleaseMethod();
		Assertions.assertNotNull(release, "release(CacheHolder,Binary)方法必须存在");
		release.setAccessible(true);
		var releaseDone = new CountDownLatch(1);
		var releaser = new Thread(() -> {
			try {
				release.invoke(gcm, holderA, KEY);
			} catch (Throwable ex) {
				// 修复前在此永久park（await断言失败）；其他异常由结果断言暴露
			} finally {
				releaseDone.countDown();
			}
		}, "UnitTest.FND_S1_3.Releaser");
		releaser.setDaemon(true);
		releaser.start();
		Assertions.assertTrue(releaseDone.await(5, TimeUnit.SECONDS), "release不能永久park（key冻结/守护停摆）");
		releaser.join(5_000);

		// 8. 无持有者后记录应被清除
		var exists = new boolean[1];
		gcm.getRocks().newProcedure(() -> {
			exists[0] = globalStates.get(KEY) != null;
			return 0L;
		}).call();
		Assertions.assertFalse(exists[0], "release后记录应被移除");
	}
}
