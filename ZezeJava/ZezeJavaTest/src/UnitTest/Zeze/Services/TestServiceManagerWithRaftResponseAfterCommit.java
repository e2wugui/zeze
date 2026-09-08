package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManagerServer;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND2-S1-1：SM-raft 七处 handler 曾用 Zeze.Transaction.Transaction.getCurrent() 注册提交前应答
 * ——该类在本派发链（dispatchRaftRequest→Procedure.call）下恒为null，runWhileCommit永不注册，
 * 应答退化为handler内立即发送（appendLog之前）：复制失败回滚后客户端已拿到rc=0假成功
 * （AllocateId号段重复发放级）。修复改用 Zeze.Raft.RocksRaft.Transaction.getCurrent()。
 * <p>
 * 用例1（红绿·确定性）：测试线程持有两个follower的Raft互斥锁 ⇒ follower的
 * processAppendEntries（持锁处理）被阻塞 ⇒ quorum确认不可达 ⇒ leader的
 * appendLog必然超时RaftRetry回滚（与TestServiceManagerWithRaftCommitThenResponse中被
 * 禁用的"close follower"法不同：锁控不依赖close()与复制应答的时序，确定性成立）。
 * 修复后：SendResult经runWhileCommit注册、_final_rollback_不触发，客户端只能拿到派发层
 * onError回的错误码；修复前（回归）：应答在handler内先于appendLog发出rc=0假成功，断言红。
 * 用例2（绿·应答后于提交）：锁释放集群恢复后，应答到达时leader的commitIndex必须已推进
 * （commitIndex在leader回调（appendLog返回）之前、也在应答发出之前推进——LogSequence
 * processAppendEntriesResult→tryApply→invokeCallback全序）。修复前应答先于appendLog发出，
 * 本断言大概率红（应答投递与quorum往返存在时序竞争，非确定性，仅作补充防线）。
 * <p>
 * FND2-S1-3：重复Login覆盖socket的userState前必须取消旧Session的keepAliveTimerTask
 * （OnSocketClose只回调最后userState的onClose）。用例1结束后被回滚的Login其handler已
 * set过userState（net层状态不随raft回滚），下一次Login覆盖它时旧定时器必须已取消——
 * 直接反射读取服务端Session的keepAliveTimerTask断言isCancelled，红绿均确定性。
 * <p>
 * 3节点raft + 裸协议客户端（对齐 TestServiceManagerWithRaftCommitThenResponse 基建）。
 * 服务端conf.keepAlivePeriod默认-1（不建定时器），setUp经反射置1000以激活Session定时器。
 */
@Fast
public class TestServiceManagerWithRaftResponseAfterCommit {
	private static final String RAFT_NAME = "rac_sm_test";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;
	private static final AtomicLong requestIds = new AtomicLong();
	private static final HashMap<String, Field> fieldCache = new HashMap<>();

	/** 裸协议raft客户端：Login/KeepAlive应答与LeaderIs广播可解码。 */
	private static final class Peer extends HandshakeClient {
		Peer(String name) throws Exception {
			super(name, new Zeze.Config());
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			// keepAlivePeriod>0时服务端周期发KeepAlive探测（rpc），必须应答否则连接被关闭
			// （对齐非raft版Agent.processKeepAlive：显式SendResultCode）。
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
					KeepAlive::new, p -> {
						p.SendResultCode(0);
						return 0;
					}, TransactionLevel.None, DispatchMode.Direct));
		}

		private Connector connector;

		AsyncSocket connect(int port) throws Exception {
			// 换端口重连前先摘除旧Connector（同host:port重复注册会抛Duplicate Connector）。
			if (connector != null) {
				getConfig().removeConnector(connector);
				connector.stop();
				connector = null;
			}
			connector = new Connector("127.0.0.1", port, false);
			getConfig().addConnector(connector);
			start();
			return connector.WaitReady();
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
		// 激活Session的keepAlive定时器（FND2-S1-3断言需要）：conf为私有final但Conf的字段
		// 是public，直接改对象字段即可。
		var confField = field("conf");
		for (var server : servers)
			((ServiceManagerServer.Conf)confField.get(server)).keepAlivePeriod = 1000;
		waitStableLeader();
		ensureLeaderReady();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		// 用例1曾长时间阻塞follower的Raft锁：重复close容错。
		for (var server : servers) {
			try {
				server.close();
			} catch (Throwable ignored) {
			}
		}
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
		// 出现 duplicate node，集群装配直接失败（3 节点测试并行时实测触发）。
		while (true) {
			try (var socket = new ServerSocket(0)) {
				if (usedPorts.add(socket.getLocalPort()))
					return socket.getLocalPort();
			}
		}
	}

	private static Rocks leaderRocks() {
		for (var rocks : rocksList)
			if (rocks.isLeader())
				return rocks;
		return null;
	}

	private static void waitStableLeader() throws Exception {
		var rocksField = field("rocks");
		for (var server : servers)
			rocksList.add((Rocks)rocksField.get(server));
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

	private static void ensureLeaderReady() throws Exception {
		var stateField = field("tableServerState");
		long deadline = System.currentTimeMillis() + 30_000;
		while (System.currentTimeMillis() < deadline) {
			var rocks = leaderRocks();
			if (rocks == null)
				continue;
			if (rocks.getRaft().isReadyLeader())
				return;
			@SuppressWarnings("unchecked")
			var table = (Zeze.Raft.RocksRaft.Table<String, Zeze.Builtin.ServiceManagerWithRaft.BServerState>)stateField
					.get(servers.get(rocksList.indexOf(rocks)));
			try {
				rocks.newProcedure(() -> {
					table.getOrAdd("UnitTest.RAC.WarmUp");
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

	private static int leaderIndex() {
		for (int i = 0; i < rocksList.size(); i++)
			if (rocksList.get(i).isLeader())
				return i;
		throw new IllegalStateException("no leader");
	}

	private static Login sendLogin(AsyncSocket sock, String sessionName, int timeoutMs) throws Exception {
		// RaftRetry(-15)：静默集群leader漂移/选举窗口的瞬态应答（appendLog同步检查isLeader，
		// 同TestServiceManagerWithRaftAllocateId.allocate的有界重试），重试耗尽才失败。
		// 每次重试必须用新Login（requestId唯一）。用例1的阻塞Login不走这里（必失败语义独立）。
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			var login = new Login();
			login.Argument.setSessionName(sessionName);
			login.getUnique().setRequestId(requestIds.incrementAndGet());
			login.setCreateTime(System.currentTimeMillis());
			login.setTimeout(timeoutMs);
			Assertions.assertTrue(login.SendForWait(sock, timeoutMs).await(timeoutMs), "login await");
			Assertions.assertFalse(login.isTimeout(), "login timeout");
			lastCode = login.getResultCode();
			if (lastCode != -15)
				return login;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("login持续RaftRetry(-15)，session=" + sessionName);
		return null; // unreachable
	}

	private static Field field(String name) throws NoSuchFieldException {
		var f = fieldCache.get(name);
		if (f == null) {
			f = ServiceManagerWithRaft.class.getDeclaredField(name);
			f.setAccessible(true);
			fieldCache.put(name, f);
		}
		return f;
	}

	/** 在指定服务器的连接上按会话名找Session（内部类，无法静态引用类型），返回其keepAlive定时器。 */
	private static Future<?> serverSessionKeepAliveTask(Zeze.Raft.Server server, String sessionName) throws Exception {
		final Future<?>[] out = new Future<?>[1];
		server.foreach(so -> {
			var us = so.getUserState();
			if (us == null || !us.getClass().getSimpleName().equals("Session"))
				return;
			var nameField = us.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			if (!sessionName.equals(nameField.get(us)))
				return;
			var taskField = us.getClass().getDeclaredField("keepAliveTimerTask");
			taskField.setAccessible(true);
			out[0] = (Future<?>)taskField.get(us);
		});
		return out[0];
	}

	@Test
	@Timeout(180)
	public void testResponseAfterCommit() throws Exception {
		var peer = new Peer("UnitTest.RAC.Reg");
		try {
			var sock = peer.connect(leaderPort());

			// warmup：集群健康时正常Login必成功（rc=0）。
			Assertions.assertEquals(0, sendLogin(sock, "UnitTest.RAC.Warm", 30_000).getResultCode(),
					"warmup login resultCode");

			// ---------------- 用例1：quorum不可达时不得假成功（确定性红绿，FND2-S1-1） ----------------
			var leaderIdx = leaderIndex();
			// 阻塞期间老leader心跳应答全部失败，解锁前/后可能退位重选：Session与后续断言必须钉在
			// 处理Login的这台服务器上，不能再用leaderRocks()（那会拿到新leader）。
			var loginServer = ((Rocks)field("rocks").get(servers.get(leaderIdx))).getRaft().getServer();
			var followerRafts = new ArrayList<Raft>();
			for (int i = 0; i < servers.size(); i++) {
				if (i != leaderIdx)
					followerRafts.add(((Rocks)field("rocks").get(servers.get(i))).getRaft());
			}
			Assertions.assertEquals(2, followerRafts.size());
			// 持有两个follower的Raft锁：processAppendEntries需持锁处理（Raft.processAppendEntries），
			// follower无法确认日志；3节点majority=2（含leader），缺两个follower的确认必不达quorum。
			// 期间follower的选举定时器（onTimer持锁）同样被阻塞，选举不可能发起。
			String blockedName = "UnitTest.RAC.Blocked." + System.nanoTime();
			for (var raft : followerRafts)
				raft.lock();
			try {
				var login = new Login();
				login.Argument.setSessionName(blockedName);
				login.getUnique().setRequestId(requestIds.incrementAndGet());
				login.setCreateTime(System.currentTimeMillis());
				login.setTimeout(15_000);
				// quorum不可达：future可能以超时异常完成（RpcTimeoutException）或拿到服务端错误码，
				// 两种形态都合法——只要不是成功码。
				try {
					login.SendForWait(sock, 15_000).await(20_000);
				} catch (RuntimeException ex) {
					// 超时/异常完成：结果码由下方断言统一裁决。
				}
				Assertions.assertNotEquals(0, login.getResultCode(),
						"quorum不可达时Login不可能完成raft提交，客户端不能拿到成功码"
								+ "（提交前应答=假成功，FND2-S1-1）");
			} finally {
				for (var raft : followerRafts)
					raft.unlock();
			}

			// ---------------- 用例2：应答到达时commitIndex必须已推进 ----------------
			waitStableLeader(); // 阻塞窗口可能引发重选，等新leader稳定
			ensureLeaderReady();
			sock = peer.connect(leaderPort()); // 连到当前leader（旧连接可能已指向follower）
			var leaderLog = ((Rocks)field("rocks").get(servers.get(leaderIndex()))).getRaft().getLogSequence();
			var before = leaderLog.getCommitIndex();
			String afterName = "UnitTest.RAC.After." + System.nanoTime();
			Assertions.assertEquals(0, sendLogin(sock, afterName, 30_000)
					.getResultCode(), "恢复后login resultCode");
			Assertions.assertTrue(leaderLog.getCommitIndex() > before,
					"应答到达时raft必须已提交（应答由_final_commit_在appendLog成功后发出，FND2-S1-1）");

			// S1-3断言（确定性红绿）：rc=0证明handler必然执行（成功提交才应答），其Session带keepAlive
			// 定时器；同一socket再次成功Login覆盖userState后，旧定时器必已取消。
			// （不依赖blocked会话做此断言：leader抖动时blocked请求可能在派发层被拒，handler未执行。）
			var afterServer = ((Rocks)field("rocks").get(servers.get(leaderIndex()))).getRaft().getServer();
			var afterTask = serverSessionKeepAliveTask(afterServer, afterName);
			Assertions.assertNotNull(afterTask, "成功login的Session必须持有keepAlive定时器");
			Assertions.assertEquals(0, sendLogin(sock, "UnitTest.RAC.Cover2." + System.nanoTime(), 30_000)
					.getResultCode(), "覆盖login resultCode");
			Assertions.assertTrue(afterTask.isCancelled(),
					"重复Login覆盖userState前必须取消旧Session的keepAliveTimerTask（FND2-S1-3）");
		} finally {
			peer.stop();
		}
	}
}
