package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Builtin.ServiceManagerWithRaft.BSession;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND5-35（A3）回归：sessionName为tSession主键兼会话归属凭证，空白名（&lt;ServiceManagerConf&gt;
 * 漏配sessionName属性时Config解析为空串）会使多个server共享同一会话行互相接管——推送错乱、
 * 断连连带注销对方注册订阅，表现为服务闪断。
 * <p>
 * 修复：(1) 服务端Login拒绝空白名（ErrorSessionName=-21，拒绝在getOrAdd之前，不产生会话行/
 * userState副作用）；(2) ServiceManagerAgentWithRaft构造器对空白名fail-fast（经Application+
 * ServiceManager=raft路径已有projectName#serverId默认，不受影响）。唯一性仍为部署契约（方案B）。
 * <p>
 * 3节点raft+裸协议客户端（对齐TestServiceManagerWithRaftCommitThenResponse基建，理由同：
 * Agent登录重试风暴与SMServer单线程化dispatch锁叠加会活锁）。
 */
@Fast
public class TestServiceManagerWithRaftLoginSessionName {
	private static final String RAFT_NAME = "fnd5_35_sm_test";
	private static final String VALID_SESSION = "UnitTest.FND5_35.Agent";

	private static final int[] ports = new int[3];
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private static final ArrayList<Rocks> rocksList = new ArrayList<>();
	private static final ArrayList<String> dbHomes = new ArrayList<>();
	private static Path raftXmlFile;
	private static final AtomicLong requestIds = new AtomicLong();
	private static final HashSet<Integer> usedPorts = new HashSet<>();

	/** 裸协议raft客户端：仅需Login应答与LeaderIs解码。 */
	private static final class Peer extends HandshakeClient {
		Peer(String name) throws Exception {
			super(name, new Zeze.Config());
			// LeaderIs：raft服务端会向所有已握手的连接推送，必须注册才能解码。
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Critical));
			// 应答工厂必须注册：不注册会被UnknownProtocol关闭连接，SendForWait永远等不到应答。
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect(int port) throws Exception {
			// WaitReady()固定5s超时：全量负载下握手可能超时，有界重试（对齐族内测试的负载加固）。
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

	private record LoginResult(long code, AsyncSocket sock) {
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
			var raftConf = RaftConfig.loadFromString(raftXmlString());
			for (var node : raftConf.getNodes().values())
				if (node.getPort() == ports[i])
					nodeNames.add(node.getName());
			servers.add(new ServiceManagerWithRaft(nodeNames.get(i), raftConf, new Zeze.Config(), false));
			// FND8-42起Raft构造器经derive私有副本联动DbHome，不再改写传入的raftConf——
			// raftConf.getDbHome()仍是xml Name而非数据目录，收尾删它是空操作，节点目录
			// 127.0.0.1_<port>因此残留。实际目录由节点名派生（derive口径，同上方预清理）。
			dbHomes.add(nodeNames.get(i).replace(':', '_'));
		}
		waitStableLeader();
	}

	@AfterAll
	@Timeout(90) // 生命周期方法默认不受类/方法级超时保护，显式兜底防拆集群挂起拖死suite
	public static void tearDown() throws Exception {
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

	@Test
	@Timeout(200)
	public void testBlankSessionNameLoginRejected() throws Exception {
		var peer = new Peer("fnd5_35_blank");
		try {
			var sock = peer.connect(leaderPort());
			// 修复前：空串/纯空白名getOrAdd出共享会话行（rc=0），同名（含均漏配）互相接管。
			var blank = sendLogin(peer, sock, "");
			Assertions.assertEquals(ServiceManagerWithRaft.ErrorSessionName, blank.code(),
					"空串sessionName必须被拒绝（FND5-35）");
			var whitespace = sendLogin(peer, blank.sock(), "   ");
			Assertions.assertEquals(ServiceManagerWithRaft.ErrorSessionName, whitespace.code(),
					"纯空白sessionName必须被拒绝（FND5-35）");

			// 拒绝发生在getOrAdd之前：tSession不得出现空白键的行（修复前空串行已落raft）。
			Assertions.assertFalse(hasBlankSessionRow(), "被拒的Login不得产生会话行");

			// 同socket上合法名字随后的Login必须成功：拒绝无userState等残留副作用。
			var valid = sendLogin(peer, whitespace.sock(), VALID_SESSION);
			Assertions.assertEquals(0, valid.code(), "拒绝空白名后合法Login必须照常成功");
		} finally {
			// 先关客户端触发服务端closeSession，定界等待会话清理收敛后再由@AfterAll拆集群
			// （对齐TestServiceManagerWithRaftEditRollback：与closeSession退避重试竞争会挂起teardown）。
			peer.stop();
			waitSessionCleaned();
		}
	}

	@Test
	public void testAgentConstructorBlankNameFailsFast() {
		// 测试目录zeze.xml的<ServiceManagerConf>漏配sessionName属性 → 解析为空串（真实的漏配形态）。
		// 修复前：构造成功，Login以空名上服务端（被拒后仅error日志，服务半死不活）。
		var config = Zeze.Config.load();
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> new ServiceManagerAgentWithRaft(config));
		Assertions.assertTrue(ex.getMessage().contains("sessionName"),
				"报错必须明确指出sessionName问题: " + ex.getMessage());
	}

	/**
	 * 发送Login并返回resultCode与实际socket（重连后可能不是传入的那个）。
	 * -15/无应答（leader漂移/请求被follower静默丢弃）时重连当前leader有界重试，
	 * 每次重试用新Login（requestId唯一），对齐TestServiceManagerWithRaftCommitThenResponse。
	 */
	private static LoginResult sendLogin(Peer peer, AsyncSocket sock, String sessionName) throws Exception {
		long lastCode = Long.MIN_VALUE;
		for (int attempt = 1; attempt <= 12; ++attempt) {
			var login = new Login();
			login.Argument.setSessionName(sessionName);
			login.getUnique().setRequestId(requestIds.incrementAndGet());
			login.setCreateTime(System.currentTimeMillis()); // 不设置会被服务端判为RaftExpired(-17)
			login.setTimeout(30_000);
			boolean dead = !login.SendForWait(sock, 30_000).await(30_000) || login.isTimeout();
			lastCode = login.getResultCode();
			if (!dead && lastCode != -15)
				return new LoginResult(lastCode, sock);
			if (attempt < 12) {
				try {
					sock = peer.connect(leaderPort()); // 重连当前leader（漂移后旧socket指向follower，请求会被丢）
				} catch (IllegalStateException e) { // leaderless窗口：留给下一轮重试
				}
				//noinspection BusyWait
				Thread.sleep(500);
			}
		}
		Assertions.fail("login重试耗尽，session=" + sessionName + "，lastCode=" + lastCode);
		return null; // unreachable
	}

	/** tSession是否残留空白键的行（任一节点上有即算）。 */
	private static boolean hasBlankSessionRow() throws Exception {
		var found = new boolean[]{false};
		for (var server : servers) {
			try {
				sessionTableOf(server).walk((name, v) -> {
					if (name.isBlank())
						found[0] = true;
					return true;
				});
			} catch (Throwable ignored) {
				// 节点重启窗口/无leader时walk失败：继续查其余节点
			}
		}
		return found[0];
	}

	/** 定界等待（≤60s）三个节点的tSession均无本测试合法会话行；超时仅记录，teardown由@Timeout兜底。 */
	private static void waitSessionCleaned() throws Exception {
		long deadline = System.currentTimeMillis() + 60_000;
		while (System.currentTimeMillis() < deadline) {
			var found = new boolean[]{false};
			for (var server : servers) {
				try {
					sessionTableOf(server).walk((name, v) -> {
						if (VALID_SESSION.equals(name))
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
		System.err.println("FND5-35 test: session row not cleaned in 60s, teardown proceeds with @Timeout guard.");
	}

	@SuppressWarnings("unchecked")
	private static Table<String, BSession> sessionTableOf(ServiceManagerWithRaft server) throws Exception {
		Field f = ServiceManagerWithRaft.class.getDeclaredField("tableSession");
		f.setAccessible(true);
		return (Table<String, BSession>)f.get(server);
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
		// Windows上快速关闭重开可能拿到同一个临时端口：去重，否则同一raft配置内出现
		// duplicate node，集群装配直接失败（3节点测试并行时实测触发）。
		while (true) {
			try (var socket = new ServerSocket(0)) {
				if (usedPorts.add(socket.getLocalPort()))
					return socket.getLocalPort();
			}
		}
	}

	private static int leaderPort() {
		for (int i = 0; i < rocksList.size(); i++)
			if (rocksList.get(i).isLeader())
				return ports[i];
		throw new IllegalStateException("no leader");
	}

	private static void waitStableLeader() throws Exception {
		var rocksField = ServiceManagerWithRaft.class.getDeclaredField("rocks");
		rocksField.setAccessible(true);
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
