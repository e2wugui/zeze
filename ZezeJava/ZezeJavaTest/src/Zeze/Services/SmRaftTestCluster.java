package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Raft.LeaderIs;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftRpc;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.jspecify.annotations.Nullable;

/** 真实三节点 SM 协议夹具。使用本夹具的类共享 ResourceLock("sm-session-cluster")。 */
final class SmRaftTestCluster implements AutoCloseable {

	// 动态采样端口段：固定段(32010-12)满负载下与上例cluster的异步关闭迟滞竞速——reuse=true
	// 已设仍bind失败=活跃listener占用（soak-20261022-1956批6中，round3成波三连红）；
	// 每实例采样互异空闲端口，彻底退出固定段竞争（GCM Release/Acquire孪生harness取端口先例）。
	// ResourceLock 保护两个使用者的生命周期，无 Application/serverId 缓存目录。
	private final int[] ports = freePorts(3);
	private static final AtomicLong REQUEST_IDS = new AtomicLong();
	private final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private final ArrayList<Rocks> rocks = new ArrayList<>();
	private final ArrayList<Peer> peers = new ArrayList<>();
	final Path directory;
	final Path configFile;

	SmRaftTestCluster() throws Exception {
		Task.tryInitThreadPool();
		directory = Files.createTempDirectory("sm-session-cluster-");
		configFile = directory.resolve("raft.xml");
		var xml = new StringBuilder("<raft Name=\"sm_session_cluster\">");
		for (int port : ports)
			xml.append("<node Host=\"127.0.0.1\" Port=\"").append(port).append("\"/>");
		xml.append("</raft>");
		Files.writeString(configFile, xml);
		try {
			var field = ServiceManagerWithRaft.class.getDeclaredField("rocks");
			field.setAccessible(true);
			for (int port : ports) {
				var conf = RaftConfig.loadFromString(xml.toString());
				conf.setDbHome(directory.resolve("node-" + port).toString());
				var server = new ServiceManagerWithRaft(
						"127.0.0.1:" + port, conf, new Config(), false);
				servers.add(server);
				rocks.add((Rocks)field.get(server));
			}
			long deadline = System.nanoTime() + 90_000_000_000L;
			while (true) {
				for (var node : rocks)
					if (requireNonNull(node.getRaft()).isReadyLeader())
						return;
				Assertions.assertTrue(System.nanoTime() < deadline, "Raft leader must become ready");
				Thread.sleep(50);
			}
		} catch (Throwable e) {
			close();
			throw e;
		}
	}

	int leaderPort() {
		for (int i = 0; i < rocks.size(); ++i)
			if (requireNonNull(rocks.get(i).getRaft()).isReadyLeader())
				return ports[i];
		throw new IllegalStateException("no ready leader");
	}

	ServiceManagerWithRaft leaderServer() {
		var port = leaderPort();
		for (int i = 0; i < ports.length; ++i)
			if (ports[i] == port)
				return servers.get(i);
		throw new IllegalStateException("no ready leader");
	}

	List<Rocks> nodes() {
		return List.copyOf(rocks);
	}

	/** 同时持有全部采样socket保证互异，finally统一释放后由raft立即重绑（TOCTOU窗口极小）。 */
	private static int[] freePorts(int count) {
		var sampled = new ArrayList<java.net.ServerSocket>();
		try {
			var result = new int[count];
			for (int i = 0; i < count; i++) {
				var socket = new java.net.ServerSocket();
				sampled.add(socket);
				socket.setReuseAddress(true);
				socket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
				result[i] = socket.getLocalPort();
			}
			return result;
		} catch (java.io.IOException e) {
			throw new IllegalStateException("sample free ports fail", e);
		} finally {
			for (var socket : sampled)
				try {
					socket.close();
				} catch (java.io.IOException ignore) {
				}
		}
	}

	Peer login(String sessionName) throws Exception {
		var peer = new Peer("SM.Session.Peer." + peers.size());
		peers.add(peer);
		// WaitReady()固定5s超时：满负载下连接建立可能超时（soak-20261022两批3中），
		// 有界重试：失败连接remove+stop后按当前leader重建（CommitThenResponse.Peer.connect同款）。
		for (int attempt = 1; ; ++attempt) {
			var connector = new Connector("127.0.0.1", leaderPort(), false);
			peer.getConfig().addConnector(connector);
			peer.start();
			try {
				peer.socket = connector.WaitReady();
				break;
			} catch (Exception e) { // 超时经Task.forceThrow sneaky-throw受检TimeoutException，编译期不可见
				if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
					throw e;
				peer.getConfig().removeConnector(connector);
				connector.stop();
				//noinspection BusyWait
				Thread.sleep(200);
			}
		}
		var login = new Login();
		login.Argument.setSessionName(sessionName);
		Assertions.assertEquals(0, peer.call(login));
		return peer;
	}

	@Override
	public void close() throws Exception {
		try {
			for (var peer : peers)
				peer.stop();
			// 先停止所有 Raft 网络，再逆序释放库，避免拆一节点时其余节点重新选举。
			for (var node : rocks)
				requireNonNull(node.getRaft()).getServer().stop();
			for (var server : servers)
				server.close();
		} finally {
			LogSequence.deleteDirectory(directory.toFile());
		}
	}

	static final class Peer extends HandshakeClient {
		@Nullable AsyncSocket socket;
		volatile boolean answerKeepAlive = true;
		volatile long keepAliveResultCode;
		final AtomicInteger keepAliveRequests = new AtomicInteger();

		Peer(String name) {
			super(name, new Config());
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
					KeepAlive::new, p -> {
						var answer = answerKeepAlive;
						var code = keepAliveResultCode;
						keepAliveRequests.incrementAndGet();
						if (answer)
							p.SendResultCode(code);
						return 0;
					}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Edit.TypeId_, new ProtocolFactoryHandle<>(
					Edit::new, p -> { p.SendResult(); return 0; }, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Subscribe.TypeId_, new ProtocolFactoryHandle<>(
					Subscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(UnSubscribe.TypeId_, new ProtocolFactoryHandle<>(
					UnSubscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		long call(RaftRpc<?, ?> request) throws Exception {
			request.getUnique().setRequestId(REQUEST_IDS.incrementAndGet());
			request.setCreateTime(System.currentTimeMillis());
			Assertions.assertTrue(request.SendForWait(requireNonNull(socket), 15_000).await(15_000), "RPC response");
			Assertions.assertFalse(request.isTimeout(), "RPC timeout");
			return request.getResultCode();
		}

		Subscribe subscribe(String serviceName) throws Exception {
			var request = new Subscribe();
			request.Argument.subs.add(new BSubscribeInfo(serviceName, 0));
			Assertions.assertEquals(0, call(request));
			return request;
		}
	}
}
