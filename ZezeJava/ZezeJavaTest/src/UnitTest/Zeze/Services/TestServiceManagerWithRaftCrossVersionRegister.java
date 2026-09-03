package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.ServiceManagerWithRaft.BServerState;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * CARRY-SMRAFT-RAFTREREG：FND-S2-3 跨版本重注册"幽灵桶残留"的 raft 版镜像。
 * <p>
 * ServiceManagerWithRaft 与非 raft 版同构地错：ProcessEditRequest 的 add 只写目标版本桶、
 * onClose 按 unReg.getVersion() 单桶删（且先删后判归属），同 identity 跨版本重注册后实例
 * 下线，raft 服务端状态与快照仍残留旧版本桶幽灵地址。
 * <p>
 * 3节点raft + 裸协议客户端（对齐 TestServiceManagerWithRaftAllocateId 基建，理由同：
 * ServiceManagerAgentWithRaft 登录重试风暴与 SMServer 单线程化 dispatch 锁叠加会活锁）。
 * 服务端桶状态经 leader 的 procedure 直读 tServerState；订阅者通知经真实 Edit 推送收集。
 */
@Fast
public class TestServiceManagerWithRaftCrossVersionRegister {
	private static final String RAFT_NAME = "crr_sm_test";
	private static final String SERVICE_NAME = "UnitTest.CRR.Service";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;
	private static final AtomicLong requestIds = new AtomicLong();

	/** 裸协议raft客户端：注册者/订阅者各一个实例，收服务端 Edit 推送。 */
	private static final class Peer extends HandshakeClient {
		final ConcurrentLinkedQueue<BEditService> editNotifies = new ConcurrentLinkedQueue<>();

		Peer(String name) throws Exception {
			super(name, new Zeze.Config());
			// LeaderIs：raft服务端会向所有已握手的连接推送，必须注册才能解码。
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			// Login/Subscribe 的应答工厂必须注册（对齐 TestServiceManagerWithRaftAllocateId 的 Peer）：
			// 不注册会被 UnknownProtocol 关闭连接，SendForWait 永远等不到应答。
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Subscribe.TypeId_, new ProtocolFactoryHandle<>(
					Subscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Edit.TypeId_, new ProtocolFactoryHandle<>(
					Edit::new, p -> {
						editNotifies.add(p.Argument);
						return 0;
					}, TransactionLevel.None, DispatchMode.Direct));
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
		waitStableLeader();
		ensureLeaderReady();
	}

	@AfterAll
	public static void tearDown() throws Exception {
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
			var table = (Table<String, BServerState>)stateField.get(servers.get(rocksList.indexOf(rocks)));
			try {
				rocks.newProcedure(() -> {
					table.getOrAdd("UnitTest.CRR.WarmUp");
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

	private static AsyncSocket connectLeader(Peer peer, String sessionName, AsyncSocket old) throws Exception {
		var port = leaderPort();
		if (old != null && !old.isClosed())
			return old; // 测试内短窗口，假定leader不漂移；漂移由重试兜底
		var sock = peer.connect(port);
		var login = new Login();
		login.Argument.setSessionName(sessionName);
		login.getUnique().setRequestId(requestIds.incrementAndGet());
		login.setCreateTime(System.currentTimeMillis());
		login.setTimeout(30_000);
		Assertions.assertTrue(login.SendForWait(sock, 30_000).await(30_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
		return sock;
	}

	/** 带限重试的Edit提交（raft抖动-15重试，对齐AllocateId测试）。 */
	private static void editForWait(Peer peer, AsyncSocket sock, BEditService arg) throws Exception {
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			var rpc = new Edit(arg);
			rpc.getUnique().setRequestId(requestIds.incrementAndGet());
			rpc.setCreateTime(System.currentTimeMillis());
			rpc.setTimeout(30_000);
			Assertions.assertTrue(rpc.SendForWait(sock, 30_000).await(30_000), "edit await");
			Assertions.assertFalse(rpc.isTimeout(), "edit timeout");
			lastCode = rpc.getResultCode();
			if (lastCode == 0)
				return;
			//noinspection BusyWait
			Thread.sleep(500);
		}
		Assertions.fail("Edit重试耗尽，lastCode=" + lastCode);
	}

	/** leader上直读tServerState的serviceName行（procedure内读，leader内存必含已提交写）。 */
	@SuppressWarnings("unchecked")
	private static BServerState readServerState(String serviceName) throws Exception {
		var rocks = leaderRocks();
		Assertions.assertNotNull(rocks, "必须有leader");
		var table = (Table<String, BServerState>)field("tableServerState").get(servers.get(rocksList.indexOf(rocks)));
		final BServerState[] out = new BServerState[1];
		rocks.newProcedure(() -> {
			out[0] = table.get(serviceName);
			return 0L;
		}).call();
		return out[0];
	}

	/** 轮询等待服务端桶状态满足条件（Edit提交/onClose均为异步raft提交）。 */
	private static BServerState waitServerState(String serviceName, long timeoutMs) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		BServerState state = null;
		while (System.currentTimeMillis() < deadline) {
			state = readServerState(serviceName);
			if (state != null)
				return state;
			//noinspection BusyWait
			Thread.sleep(200);
		}
		return state;
	}

	private static boolean bucketContainsIdentity(BServerState state, long version, String identity) {
		if (state == null)
			return false;
		var versions = state.getServiceInfosVersion().get(version);
		return versions != null && versions.getServiceInfos().containsKey(identity);
	}

	/** 轮询等待桶内出现 identity：Edit 的应答在 raft 提交后即发出，但读侧节点的本地
	 * apply 可能滞后（commit!=applied），一次读取存在竞态窗口。 */
	private static void waitBucketContainsIdentity(String serviceName, long version, String identity,
												   long timeoutMs, String msg) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (bucketContainsIdentity(readServerState(serviceName), version, identity))
				return;
			//noinspection BusyWait
			Thread.sleep(100);
		}
		Assertions.fail(msg + ": v" + version + " 桶未见 identity=" + identity);
	}

	/** 等待订阅者收到一条Edit通知满足条件。 */
	private static void waitForNotify(Peer peer, java.util.function.Predicate<BEditService> cond,
									  String msg, long timeoutMs) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			for (var edit : peer.editNotifies)
				if (cond.test(edit))
					return;
			//noinspection BusyWait
			Thread.sleep(100);
		}
		Assertions.fail(msg);
	}

	// ------------------------- 用例1：跨版本重注册→下线无幽灵 -------------------------

	@Test
	@Timeout(150)
	public void testCrossVersionReregisterThenOfflineNoGhost() throws Exception {
		var reg = new Peer("UnitTest.CRR.Reg");
		var sub = new Peer("UnitTest.CRR.Sub");
		try {
			var regSock = connectLeader(reg, "UnitTest.CRR.Reg", null);
			var subSock = connectLeader(sub, "UnitTest.CRR.Sub", null);

			// 订阅全部版本（version=0）
			var info = new BSubscribeInfo(SERVICE_NAME);
			var subArg = new BSubscribeArgument();
			subArg.subs.add(info);
			var subscribe = new Subscribe(subArg);
			subscribe.getUnique().setRequestId(requestIds.incrementAndGet());
			subscribe.setCreateTime(System.currentTimeMillis());
			subscribe.setTimeout(30_000);
			Assertions.assertTrue(subscribe.SendForWait(subSock, 30_000).await(30_000), "subscribe await");
			Assertions.assertEquals(0, subscribe.getResultCode(), "subscribe resultCode");
			sub.editNotifies.clear();

			// 注册v5
			var info5 = new BServiceInfo(SERVICE_NAME, "1", 5, "127.0.0.1", 1005);
			var add5 = new BEditService();
			add5.getAdd().add(info5);
			editForWait(reg, regSock, add5);
			waitForNotify(sub, e -> e.getAdd().contains(info5), "订阅者必须收到add(info5)", 10_000);
			waitBucketContainsIdentity(SERVICE_NAME, 5, "1", 10_000, "v5桶必须有identity=1");

			// 同identity重注册到v6：修复后旧版本桶(v5)必须被清理并通知remove(info5)
			var info6 = new BServiceInfo(SERVICE_NAME, "1", 6, "127.0.0.1", 1006);
			var add6 = new BEditService();
			add6.getAdd().add(info6);
			editForWait(reg, regSock, add6);
			waitForNotify(sub, e -> e.getRemove().contains(info5), "跨版本重注册必须向订阅者推送remove(info5)", 10_000);

			long deadline = System.currentTimeMillis() + 10_000;
			while (System.currentTimeMillis() < deadline) {
				var state = readServerState(SERVICE_NAME);
				if (!bucketContainsIdentity(state, 5, "1") && bucketContainsIdentity(state, 6, "1"))
					break;
				//noinspection BusyWait
				Thread.sleep(200);
			}
			Assertions.assertFalse(bucketContainsIdentity(readServerState(SERVICE_NAME), 5, "1"),
					"跨版本重注册后raft服务端旧版本桶(v5)不能残留identity（幽灵）");
			Assertions.assertTrue(bucketContainsIdentity(readServerState(SERVICE_NAME), 6, "1"),
					"新版本桶(v6)必须有identity=1");

			// 注册者断连→onClose注销（会话registers只保留最后一次注册info6）：
			// 修复前只删v6桶，v5残留幽灵；修复后全版本桶清空。
			regSock.close();
			waitForNotify(sub, e -> e.getRemove().contains(info6), "实例下线必须推送remove(info6)", 15_000);

			deadline = System.currentTimeMillis() + 15_000;
			while (System.currentTimeMillis() < deadline) {
				var state = readServerState(SERVICE_NAME);
				if (!bucketContainsIdentity(state, 5, "1") && !bucketContainsIdentity(state, 6, "1"))
					break;
				//noinspection BusyWait
				Thread.sleep(200);
			}
			var finalState = readServerState(SERVICE_NAME);
			Assertions.assertFalse(bucketContainsIdentity(finalState, 5, "1"), "下线后v5桶不能残留幽灵");
			Assertions.assertFalse(bucketContainsIdentity(finalState, 6, "1"), "下线后v6桶必须清空");

			// 新订阅者的快照不含幽灵
			var subArg2 = new BSubscribeArgument();
			subArg2.subs.add(new BSubscribeInfo(SERVICE_NAME));
			var subscribe2 = new Subscribe(subArg2);
			subscribe2.getUnique().setRequestId(requestIds.incrementAndGet());
			subscribe2.setCreateTime(System.currentTimeMillis());
			subscribe2.setTimeout(30_000);
			Assertions.assertTrue(subscribe2.SendForWait(subSock, 30_000).await(30_000), "subscribe2 await");
			Assertions.assertEquals(0, subscribe2.getResultCode(), "subscribe2 resultCode");
			BServiceInfosVersion snapshot = subscribe2.Result.map.get(SERVICE_NAME);
			if (snapshot != null) {
				// BServiceInfosVersion 无按 identity 查询：遍历全部版本桶，任何桶都不得残留该 identity。
				var ghost = new java.util.concurrent.atomic.AtomicBoolean(false);
				for (var it = snapshot.getInfosIterator(); it.moveToNext(); )
					for (var si : it.value().getSortedIdentities())
						if ("1".equals(si.getServiceIdentity()))
							ghost.set(true);
				Assertions.assertFalse(ghost.get(), "新订阅者快照不能包含幽灵identity");
			}
		} finally {
			reg.stop();
			sub.stop();
		}
	}

	// ------------------------- 用例2：注销归属守卫 -------------------------

	@Test
	@Timeout(150)
	public void testRemoveHonorsSessionOwnership() throws Exception {
		var old = new Peer("UnitTest.CRR.Old");
		var neu = new Peer("UnitTest.CRR.New");
		try {
			var oldSock = connectLeader(old, "UnitTest.CRR.Old", null);
			var neuSock = connectLeader(neu, "UnitTest.CRR.New", null);

			// 旧会话注册 identity=1
			var infoA = new BServiceInfo(SERVICE_NAME, "1", 5, "127.0.0.1", 1005);
			var addA = new BEditService();
			addA.getAdd().add(infoA);
			editForWait(old, oldSock, addA);
			waitBucketContainsIdentity(SERVICE_NAME, 5, "1", 10_000, "旧会话注册后v5桶必须有identity=1");

			// 新会话AddOrUpdate同identity（同版本，ip不同）
			var infoB = new BServiceInfo(SERVICE_NAME, "1", 5, "127.0.0.2", 2005);
			var addB = new BEditService();
			addB.getAdd().add(infoB);
			editForWait(neu, neuSock, addB);
			waitServerState(SERVICE_NAME, 10_000);

			// 旧会话注销：修复前无条件删桶记录（归属不符也删，静默吞掉新会话注册）；修复后忽略。
			var rm = new BEditService();
			rm.getRemove().add(infoA);
			editForWait(old, oldSock, rm);

			long deadline = System.currentTimeMillis() + 10_000;
			String owner = null;
			while (System.currentTimeMillis() < deadline) {
				var state = readServerState(SERVICE_NAME);
				var versions = state != null ? state.getServiceInfosVersion().get(5L) : null;
				var exist = versions != null ? versions.getServiceInfos().get("1") : null;
				if (exist == null)
					break; // 已被移除
				owner = exist.getSessionName();
				if ("UnitTest.CRR.New".equals(owner))
					break;
				//noinspection BusyWait
				Thread.sleep(200);
			}
			var state = readServerState(SERVICE_NAME);
			var versions = state != null ? state.getServiceInfosVersion().get(5L) : null;
			var exist = versions != null ? versions.getServiceInfos().get("1") : null;
			Assertions.assertNotNull(exist, "旧会话的注销不能删除新会话已AddOrUpdate的注册");
			Assertions.assertEquals("UnitTest.CRR.New", exist.getSessionName(), "桶内记录必须归属新会话");

			// 清理：新会话注销生效
			var rm2 = new BEditService();
			rm2.getRemove().add(infoB);
			editForWait(neu, neuSock, rm2);
			deadline = System.currentTimeMillis() + 10_000;
			while (System.currentTimeMillis() < deadline) {
				if (!bucketContainsIdentity(readServerState(SERVICE_NAME), 5, "1"))
					break;
				//noinspection BusyWait
				Thread.sleep(200);
			}
			Assertions.assertFalse(bucketContainsIdentity(readServerState(SERVICE_NAME), 5, "1"),
					"属主会话注销必须移除记录");
		} finally {
			old.stop();
			neu.stop();
		}
	}

	// 复用反射字段缓存（每用例独立Peer，字段反射经缓存重做，成本可忽略）
	private static final HashMap<String, Field> fieldCache = new HashMap<>();

	private static Field field(String name) throws NoSuchFieldException {
		var f = fieldCache.get(name);
		if (f == null) {
			f = ServiceManagerWithRaft.class.getDeclaredField(name);
			f.setAccessible(true);
			fieldCache.put(name, f);
		}
		return f;
	}
}
