package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND-S1-5 基本功能回归：SM-raft ProcessAllocateIdRequest 的号段应答从"事务内立即SendResult"
 * 改为"raft提交成功后发送"（runWhileCommit挂到提交动作上，回滚路径由派发层onError回发错误码）。
 * <p>
 * RaftRetry回滚窗口的号段重复发放需要集群故障注入，无法确定性构造（竞态档，修复以推演核查为主）；
 * 本测试守护修改的最基本承诺：经真实派发路径，AllocateId仍能在提交后收到成功应答，且号段连续
 * 不重不漏——若提交动作挂接位置错误（应答丢失/提前/重复），本测试直接失败。
 * <p>
 * 说明：不使用ServiceManagerAgentWithRaft——它的登录重试风暴与SMServer的"单线程化"dispatch锁
 * 叠加会活锁（登录appendLog的等待期间AppendEntries应答被锁挡住，复制停摆，-15循环），改用裸协议
 * 客户端单发请求（Login/AllocateId走真实raft派发路径）。
 * 3节点raft（Raft构造强制>=3），随机端口避免并行测试冲突。
 */
@Fast
public class TestServiceManagerWithRaftAllocateId {
	private static final String RAFT_NAME = "s15_sm_test";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Zeze.Raft.RocksRaft.Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;
	private static Peer client;
	private static final AtomicLong requestIds = new AtomicLong();

	/** 裸协议raft客户端：Login/AllocateId收发；LeaderIs必须注册（服务端会主动推送）。 */
	private static final class Peer extends HandshakeClient {
		Peer(String name) {
			super(name, new Zeze.Config());
			// LeaderIs：raft服务端会向所有已握手的连接推送（重定向/通告），必须注册才能解码，
			// 否则连接被UnknownProtocol关闭（对齐Raft.Agent.NetClient的注册）。
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
					AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
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
		// 进程内3节点raft+客户端共享Selectors，默认1个selector线程不够用：
		// SMServer的dispatchRaftRequest在调用线程(=selector IO线程)同步执行procedure，
		// appendLog等待期间该IO循环被冻结，AppendEntries应答无法处理→复制自死锁
		// （参见GlobalCacheManagerAsyncServer.main的相同处理）。
		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (Zeze.Net.Selectors.getInstance().getCount() < cpuCount)
			Zeze.Net.Selectors.getInstance().add(cpuCount - Zeze.Net.Selectors.getInstance().getCount());
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
			servers.add(new ServiceManagerWithRaft(nodeNames.get(i), raftConf, new Zeze.Config(), false));
			dbHomes.add(raftConf.getDbHome());
		}
		waitStableLeader(); // 本机默认定时器下选举初期会抖动十余秒再收敛
		ensureLeaderReady();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (client != null)
			client.stop();
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

	private static int freePort() throws Exception {
		try (var socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static Zeze.Raft.RocksRaft.Rocks leaderRocks() {
		for (int i = 0; i < servers.size(); i++)
			if (rocksList.get(i).isLeader())
				return rocksList.get(i);
		return null;
	}

	/** 同一节点连续isLeader()满5秒视为稳定：本机默认定时器下选举初期会抖动十余秒再收敛。 */
	private static void waitStableLeader() throws Exception {
		var rocksField = ServiceManagerWithRaft.class.getDeclaredField("rocks");
		rocksField.setAccessible(true);
		for (var server : servers)
			rocksList.add((Zeze.Raft.RocksRaft.Rocks)rocksField.get(server));
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

	/**
	 * 静默集群上，选举时写入的SetLeaderReadyEvent可能因初始发送被跳过（socket未ready/stale pending）
	 * 而搁置：leader保持当选但never-ready，waitLeaderReady()永久阻塞所有wire请求。用一个raft写
	 * 强制触发trySendAppendEntries全量重发，解开搁置并等待ready。
	 */
	private static void ensureLeaderReady() throws Exception {
		var autoKeyField = ServiceManagerWithRaft.class.getDeclaredField("tableAutoKey");
		autoKeyField.setAccessible(true);
		long deadline = System.currentTimeMillis() + 30_000;
		while (System.currentTimeMillis() < deadline) {
			var rocks = leaderRocks();
			if (rocks == null)
				continue;
			if (rocks.getRaft().isReadyLeader())
				return;
			@SuppressWarnings("unchecked")
			var table = (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BAutoKey>)
					autoKeyField.get(servers.get(rocksList.indexOf(rocks)));
			try {
				rocks.newProcedure(() -> {
					table.put("UnitTest.FND_S1_5.WarmUp", new Zeze.Builtin.ServiceManagerWithRaft.BAutoKey());
					return 0L;
				}).call();
			} catch (Throwable ex) {
				// 抖动期的RaftRetry/异常，重试
			}
			//noinspection BusyWait
			Thread.sleep(200);
		}
		Assertions.fail("30s内leader未ready");
	}

	private static int leaderPort() {
		for (int i = 0; i < rocksList.size(); i++)
			if (rocksList.get(i).isLeader())
				return ports[i];
		throw new IllegalStateException("no leader");
	}

	@Test
	@Timeout(150)
	public void testAllocateIdCommitThenResponse() throws Exception {
		client = new Peer("UnitTest.FND_S1_5.Agent");
		login(ensureLeaderSocket());

		var name = "UnitTest.FND_S1_5.AutoKey";
		var start1 = allocate(name, 100);
		var start2 = allocate(name, 100);

		Assertions.assertTrue(start1 >= 0, "startId必须有效");
		Assertions.assertEquals(start1 + 100, start2, "连续两次AllocateId的号段必须连续不重叠");
	}

	private static AsyncSocket socket;
	private static int socketPort = -1;

	/**
	 * 静默集群的leader会漂移：客户端socket若还连着旧leader，AllocateId会被立即
	 * RaftRetry(-15)拒绝（appendLog同步检查isLeader）。每次重试前对齐现任leader：
	 * 端口变了就重连（会话绑定在socket上，须重新login）。
	 */
	private static AsyncSocket ensureLeaderSocket() throws Exception {
		var port = leaderPort();
		if (socket != null && socketPort == port && !socket.isClosed())
			return socket;
		socket = client.connect(port);
		socketPort = port;
		login(socket);
		return socket;
	}

	private static void login(AsyncSocket sock) throws Exception {
		var login = new Login();
		login.Argument.setSessionName("UnitTest.FND_S1_5.Agent");
		login.getUnique().setRequestId(requestIds.incrementAndGet());
		login.setCreateTime(System.currentTimeMillis()); // 不设置会被服务端判为RaftExpired(-17)
		login.setTimeout(30_000);
		Assertions.assertTrue(login.SendForWait(sock, 30_000).await(30_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
	}

	/**
	 * 走真实raft派发路径（dispatchRaftRequest→processRequest→ProcessAllocateIdRequest）。
	 * 本机静默集群的follower会周期性漂移回pre-vote，单次appendLog可能-15（RaftRetry，
	 * 提交动作未发出应答、由派发层onError回发错误码）；leader漂移时对齐现任leader
	 * 后重试。带限重试，重试耗尽才失败。
	 */
	private static long allocate(String name, int count) throws Exception {
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			var rpc = new AllocateId();
			rpc.Argument.setName(name);
			rpc.Argument.setCount(count);
			rpc.getUnique().setRequestId(requestIds.incrementAndGet());
			rpc.setCreateTime(System.currentTimeMillis());
			rpc.setTimeout(30_000);
			var sock = ensureLeaderSocket();
			Assertions.assertTrue(rpc.SendForWait(sock, 30_000).await(30_000),
					"AllocateId必须收到应答（提交后或错误码）");
			Assertions.assertFalse(rpc.isTimeout(), "AllocateId不能只超时");
			lastCode = rpc.getResultCode();
			if (lastCode == 0) {
				Assertions.assertEquals(count, rpc.Result.getCount(), "AllocateId count");
				return rpc.Result.getStartId();
			}
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("AllocateId重试耗尽，lastCode=" + lastCode);
		return -1; // unreachable
	}
}
