package Zeze.Services;

import java.io.File;
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
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND15 svc-01 红绿钉板（raft版）：tAutoKey 持久键的唯一名上限——raft 表逐出不可移植
 * （BAutoKey.Current是已交付位置，删行重建会重发号段），超限只能拒绝（ErrorRequestId）。
 * 修复前无任何唯一名限制，攻击name无界成为raft复制表行（全节点+WAL放大）。
 * <p>
 * walkKey计数语义：已有唯一名&gt;=MAX_UNIQUE_NAMES 时新name拒绝（上限=MAX个唯一name）。
 * 填满按实测行数补足到恰好MAX行（warmup的空bean put不落行——RocksRaft空值put无效化，
 * warmup只为触发append全发；数行补满对warmup占行与否免疫）。
 * 既有name在满员后必须继续可用（拒绝只针对新name）。
 * <p>
 * 基建对齐TestServiceManagerWithRaftAllocateId（3节点raft+裸协议客户端；不用
 * ServiceManagerAgentWithRaft——登录重试风暴与SMServer单线程化dispatch锁叠加会活锁）。
 */
@Fast
public class TestRaftUniqueNamesLimit {

	/** Raft fatalKill 守卫（halt(-1) 杀死测试 JVM，见 SmRaftFatalGuard）。 */
	private static final SmRaftFatalGuard fatalGuard = new SmRaftFatalGuard();
	private static final String RAFT_NAME = "raft_unique_names_test";
	private static final String SESSION_NAME = "UnitTest.RaftUniqueNames.Agent";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Zeze.Raft.RocksRaft.Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;
	private static Peer client;
	private static final AtomicLong requestIds = new AtomicLong();

	private static final class Peer extends HandshakeClient {
		Peer(String name) {
			super(name, new Zeze.Config());
			// LeaderIs：raft服务端会向所有已握手的连接推送（重定向/通告），必须注册才能解码。
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
					AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect(int port) throws Exception {
			// WaitReady固定5s超时：loopback connect偶发SYN无应答/全量负载下握手超时，
			// 有界重试：失败连接remove+stop后重建（对齐族内Peer.connect的负载加固）。
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
		// 进程内3节点raft+客户端共享Selectors：SMServer的dispatchRaftRequest在调用线程同步
		// 执行procedure，appendLog等待期间该IO循环被冻结会复制自死锁（族内基建同款扩容）。
		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (Zeze.Net.Selectors.getInstance().getCount() < cpuCount)
			Zeze.Net.Selectors.getInstance().add(cpuCount - Zeze.Net.Selectors.getInstance().getCount());
		for (int i = 0; i < ports.length; i++)
			ports[i] = freePort();
		raftXmlFile = Files.createTempFile(RAFT_NAME, ".xml");
		Files.writeString(raftXmlFile, raftXmlString());
		var nodeNames = new ArrayList<String>();
		// Windows下启动前清理，保证全新状态（freePort复用端口号时Raft会打开含异构raft日志的
		// 残留目录，leader重发/apply时decode撞unknown table template，集群永久卡死）。族内先例：
		// TestServiceManagerWithRaftSuspect.cleanDirs。
		for (int port : ports) {
			LogSequence.deleteDirectory(new File("127.0.0.1_" + port));
			LogSequence.deleteDirectory(new File(RAFT_NAME + "_127.0.0.1_" + port));
		}
		for (int i = 0; i < ports.length; i++) {
			var raftConf = RaftConfig.loadFromString(raftXmlString());
			for (var node : raftConf.getNodes().values())
				if (node.getPort() == ports[i])
					nodeNames.add(node.getName());
			servers.add(fatalGuard.install(new ServiceManagerWithRaft(nodeNames.get(i), raftConf, new Zeze.Config(), false)));
			dbHomes.add(nodeNames.get(i).replace(':', '_'));
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
		rocksList.clear();
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
		// Windows 上快速关闭重开可能拿到同一个临时端口：去重，否则同一 raft 配置内
		// 出现 duplicate node，集群装配直接失败。
		while (true) {
			try (var socket = new ServerSocket(0)) {
				if (usedPorts.add(socket.getLocalPort()))
					return socket.getLocalPort();
			}
		}
	}

	private static Zeze.Raft.RocksRaft.Rocks leaderRocks() {
		for (var rocks : rocksList)
			if (rocks.isLeader())
				return rocks;
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
	 * warmup写入一行tAutoKey（该行计入唯一名上限，见类注释的计数说明），并用raft写
	 * 强制触发trySendAppendEntries全发，解开never-ready搁置（族内基建同款）。
	 */
	private static void ensureLeaderReady() throws Exception {
		var autoKeyField = ServiceManagerWithRaft.class.getDeclaredField("tableAutoKey");
		autoKeyField.setAccessible(true);
		long deadline = System.currentTimeMillis() + 90_000;
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
					table.put("UnitTest.RaftUniqueNames.WarmUp", new Zeze.Builtin.ServiceManagerWithRaft.BAutoKey());
					return 0L;
				}).call();
			} catch (Throwable ex) {
				// 抖动期的RaftRetry/异常，重试
			}
			//noinspection BusyWait
			Thread.sleep(200);
		}
		Assertions.fail("90s内leader未ready");
	}

	private static int leaderPort() {
		for (int i = 0; i < rocksList.size(); i++)
			if (rocksList.get(i).isLeader())
				return ports[i];
		throw new IllegalStateException("no leader");
	}

	private static AsyncSocket socket;
	private static int socketPort = -1;

	/**
	 * 静默集群的leader会漂移：客户端socket若还连着旧leader，请求会被RaftRetry(-15)。
	 * 每次发送前对齐现任leader：端口变了就重连（会话绑定在socket上，须重新login）。
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
		login.Argument.setSessionName(SESSION_NAME);
		login.getUnique().setRequestId(requestIds.incrementAndGet());
		login.setCreateTime(System.currentTimeMillis()); // 不设置会被服务端判为RaftExpired(-17)
		login.setTimeout(30_000);
		Assertions.assertTrue(login.SendForWait(sock, 30_000).await(30_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
	}

	/** 走真实raft派发路径发送并返回resultCode（单次语义；RaftRetry漂移由调用方重试）。 */
	private static long allocateOnce(String name, int count) throws Exception {
		var rpc = new AllocateId();
		rpc.Argument.setName(name);
		rpc.Argument.setCount(count);
		rpc.getUnique().setRequestId(requestIds.incrementAndGet());
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setTimeout(30_000);
		var sock = ensureLeaderSocket();
		Assertions.assertTrue(rpc.SendForWait(sock, 30_000).await(30_000), "alloc await: " + name);
		Assertions.assertFalse(rpc.isTimeout(), "alloc timeout: " + name);
		return rpc.getResultCode();
	}

	/** rc==0有界重试（族内allocate形态）：漂移期-15重发，成功返回startId。 */
	private static long allocate(String name, int count) throws Exception {
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			lastCode = allocateOnce(name, count);
			if (lastCode == 0)
				return 0;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("AllocateId重试耗尽，name=" + name + " lastCode=" + lastCode);
		return -1; // unreachable
	}

	@Test
	@Timeout(300)
	public void testRaftUniqueNamesExceededReject() throws Exception {
		client = new Peer(SESSION_NAME);
		login(ensureLeaderSocket());

		var max = Id128UdpServer.MAX_UNIQUE_NAMES;
		// 填满上限：按实测行数补足到恰好MAX行（warmup的空bean put不落行——RocksRaft空值
		// put无效化，warmup只为触发append全发；不依赖该行为细节，数行补满对warmup占行与否免疫）。
		var rows = countAutoKeyRows();
		for (int i = 0; i < max - rows; i++)
			allocate("raftnames-name-" + i, 1);
		Assertions.assertEquals(max, countAutoKeyRows(), "填满后表行数必须恰好达到上限");

		// 第MAX+1个唯一名：拒绝（ErrorRequestId；raft无逐出，超限即拒绝）。
		// -15（漂移）重试后仍必须是确定性的-13。
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			lastCode = allocateOnce("raftnames-overflow", 1);
			if (lastCode != -15)
				break;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.assertEquals(Procedure.ErrorRequestId, lastCode,
				"唯一名超限必须确定性拒绝（非漂移瞬态码）");

		// 满员后既有name必须继续可用（拒绝只针对新name）。
		Assertions.assertEquals(0, allocate("raftnames-name-0", 1), "既有name满员后必须可用");
	}

	/** leader本地procedure直读tAutoKey行数（walkKey全量计数；只读不产生日志）。 */
	private static int countAutoKeyRows() throws Exception {
		var autoKeyField = ServiceManagerWithRaft.class.getDeclaredField("tableAutoKey");
		autoKeyField.setAccessible(true);
		var leader = leaderRocks();
		Assertions.assertNotNull(leader, "必须有leader");
		@SuppressWarnings("unchecked")
		var table = (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BAutoKey>)
				autoKeyField.get(servers.get(rocksList.indexOf(leader)));
		var cnt = new int[1];
		leader.newProcedure(() -> {
			table.walkKey(k -> {
				cnt[0]++;
				return true;
			});
			return 0L;
		}).call();
		return cnt[0];
	}
}
