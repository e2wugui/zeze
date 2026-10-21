package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
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

	// 专用端口段；ResourceLock 保护两个使用者的生命周期，无 Application/serverId 缓存目录。
	private static final int[] PORTS = {32010, 32011, 32012};
	private static final AtomicLong REQUEST_IDS = new AtomicLong();
	private final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();
	private final ArrayList<Rocks> rocks = new ArrayList<>();
	private final ArrayList<Peer> peers = new ArrayList<>();
	private final SmRaftFatalGuard fatalGuard = new SmRaftFatalGuard();
	final Path directory;
	final Path configFile;

	SmRaftTestCluster() throws Exception {
		Task.tryInitThreadPool();
		directory = Files.createTempDirectory("sm-session-cluster-");
		configFile = directory.resolve("raft.xml");
		var xml = new StringBuilder("<raft Name=\"sm_session_cluster\">");
		for (int port : PORTS)
			xml.append("<node Host=\"127.0.0.1\" Port=\"").append(port).append("\"/>");
		xml.append("</raft>");
		Files.writeString(configFile, xml);
		try {
			var field = ServiceManagerWithRaft.class.getDeclaredField("rocks");
			field.setAccessible(true);
			for (int port : PORTS) {
				var conf = RaftConfig.loadFromString(xml.toString());
				conf.setDbHome(directory.resolve("node-" + port).toString());
				var server = fatalGuard.install(new ServiceManagerWithRaft(
						"127.0.0.1:" + port, conf, new Config(), false));
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
				return PORTS[i];
		throw new IllegalStateException("no ready leader");
	}

	Peer login(String sessionName) throws Exception {
		var peer = new Peer("SM.Session.Peer." + peers.size());
		peers.add(peer);
		var connector = new Connector("127.0.0.1", leaderPort(), false);
		peer.getConfig().addConnector(connector);
		peer.start();
		peer.socket = connector.WaitReady();
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
		Assertions.assertEquals(0, fatalGuard.fatalKills.get(), "no Raft fatalKill");
	}

	static final class Peer extends HandshakeClient {
		@Nullable AsyncSocket socket;

		Peer(String name) {
			super(name, new Config());
			AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(
					LeaderIs::new, p -> 0, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
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
