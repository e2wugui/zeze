package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import Zeze.Builtin.ServiceManagerWithRaft.BSession;
import Zeze.Builtin.ServiceManagerWithRaft.BServerState;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Net.Service;
import Zeze.Raft.GetLeader;
import Zeze.Raft.Raft;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.StartServerConnector;
import Zeze.Raft.StopServerConnector;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import harness.Fast;
import harness.ForkedJvm;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

/** 超时日志仍未决时，控制和探活继续工作，业务增量须等前序日志应用后重新计算。 */
@Fast
@ForkedJvm
@Timeout(150)
@ResourceLock("sm-session-cluster")
public class TestServiceManagerWithRaftPendingLogDispatch {
	private static final String SERVICE = "SM.PendingLog.Service";
	private static final long VERSION = 7;

	@Test
	void managementRequestsRunWhileRemovalIsPending() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var peer = cluster.login("SM.PendingLog.Management");
			peer.AddFactoryHandle(GetLeader.TypeId_, new Service.ProtocolFactoryHandle<>(
					GetLeader::new, null, TransactionLevel.None, DispatchMode.Direct));
			peer.AddFactoryHandle(StopServerConnector.TypeId_, new Service.ProtocolFactoryHandle<>(
					StopServerConnector::new, null, TransactionLevel.None, DispatchMode.Direct));
			peer.AddFactoryHandle(StartServerConnector.TypeId_, new Service.ProtocolFactoryHandle<>(
					StartServerConnector::new, null, TransactionLevel.None, DispatchMode.Direct));
			var info = info("1");
			add(peer, info);
			try (var pending = new PendingRemoval(cluster, peer, info)) {
				var get = new GetLeader();
				Assertions.assertEquals(0, peer.call(get), "未决日志不能阻止查询Leader");
				Assertions.assertEquals(0, peer.call(new StopServerConnector()), "未决日志不能阻止停止连接");
				pending.leader.getServer().getConfig().forEachConnector(c ->
						Assertions.assertNull(c.getSocket(), "停止请求必须实际停止Connector"));
				Assertions.assertEquals(0, peer.call(new StartServerConnector()), "未决日志不能阻止恢复连接");
				pending.leader.getServer().getConfig().forEachConnector(c ->
						Assertions.assertNotNull(c.getSocket(), "启动请求必须实际重建Connector"));
				pending.assertPending();
			}
			awaitReplicas(cluster, null);
		}
	}

	@Test
	void keepAliveFailureClosesConnectionWhileRemovalIsPending() throws Exception {
		assertKeepAliveClosesConnection(false);
	}

	@Test
	void keepAliveTimeoutClosesConnectionWhileRemovalIsPending() throws Exception {
		assertKeepAliveClosesConnection(true);
	}

	private static void assertKeepAliveClosesConnection(boolean timeout) throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var sessionName = "SM.PendingLog.KeepAlive";
			var server = cluster.leaderServer();
			var peer = cluster.login(sessionName);
			var info = info("1");
			add(peer, info);
			try (var pending = new PendingRemoval(cluster, peer, info)) {
				var socket = requireNonNull(peer.socket);
				// 在删除已经超时后才启动真实Session探活，排除appendLog持SM锁期间
				// 积累的旧探活超时，确保关闭确实来自本测试注入的失败/超时。
				startKeepAlive(server, pending.leader, sessionName);
				await(() -> peer.keepAliveRequests.get() > 0, 5_000, "必须先完成成功探活阶段");
				Assertions.assertFalse(socket.isClosed(), "成功探活应保持连接");
				int received = peer.keepAliveRequests.get();
				if (timeout)
					peer.answerKeepAlive = false;
				else
					peer.keepAliveResultCode = Procedure.LogicError;
				await(() -> socket.isClosed(), 12_000,
						timeout ? "未决日志期间KeepAlive超时仍应关闭连接" : "未决日志期间KeepAlive失败仍应关闭连接");
				Assertions.assertTrue(peer.keepAliveRequests.get() > received, "必须收到故障阶段的真实探活请求");
				pending.assertPending();
			}
			awaitReplicas(cluster, null);
		}
	}

	private static void startKeepAlive(ServiceManagerWithRaft server, Raft raft, String sessionName) throws Exception {
		var rocks = (Rocks)raft.getStateMachine();
		var table = requireNonNull(rocks.<String, BSession>getTableTemplate("tSession")).openTable();
		var sessionId = new long[1];
		table.walk((name, row) -> {
			if (sessionName.equals(name))
				sessionId[0] = row.getSessionId();
			return true;
		});
		var socket = requireNonNull(raft.getServer().GetSocket(sessionId[0]));
		var field = ServiceManagerWithRaft.class.getDeclaredField("conf");
		field.setAccessible(true);
		server.lock();
		try {
			((ServiceManagerServer.Conf)field.get(server)).keepAlivePeriod = 100;
			var previous = (ServiceManagerWithRaft.Session)socket.getUserState();
			previous.cancelKeepAlive();
			socket.setUserState(server.new Session(sessionName, sessionId[0]));
		} finally {
			server.unlock();
		}
	}

	@Test
	void sameServiceAndVersionCanRegisterAgainAfterPendingLastRemoval() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var peer = cluster.login("SM.PendingLog.Publisher");
			var oldInfo = info("1");
			var newInfo = info("2");
			add(peer, oldInfo);
			awaitReplicas(cluster, oldInfo);
			try (var pending = new PendingRemoval(cluster, peer, oldInfo)) {
				// 旧状态仍有v7桶；若放行，会产生修改已有v7 Bean的增量，而前序删除将移除此桶。
				Assertions.assertTrue(replicasContain(cluster, oldInfo), "超时删除尚未应用，基线仍包含旧版本桶");
				var retry = new Edit();
				retry.Argument.getAdd().add(newInfo);
				Assertions.assertEquals(Procedure.RaftRetry, peer.call(retry), "须拒绝基于旧版本桶生成增量");
				pending.assertPending();
			}
			// 恢复多数派后，迟到的删除必须真正落到全部节点，不能只验证返回了重试码。
			awaitReplicas(cluster, null);
			var current = cluster.login("SM.PendingLog.Publisher");
			add(current, newInfo);
			awaitReplicas(cluster, newInfo);
			var snapshot = current.subscribe(SERVICE).Result.map.get(SERVICE);
			var infos = requireNonNull(snapshot.getInfos(VERSION));
			Assertions.assertNull(infos.findServiceInfoByIdentity(oldInfo.getServiceIdentity()));
			Assertions.assertNotNull(infos.findServiceInfoByIdentity(newInfo.getServiceIdentity()));
		}
	}

	private static BServiceInfo info(String identity) {
		return new BServiceInfo(SERVICE, identity, VERSION, "127.0.0.1", 7707);
	}

	private static void add(SmRaftTestCluster.Peer peer, BServiceInfo info) throws Exception {
		var request = new Edit();
		request.Argument.getAdd().add(info);
		Assertions.assertEquals(0, peer.call(request));
	}

	/** 持住两个follower的Raft锁，使复制不能确认且选举不能越过故障窗口。 */
	private static final class PendingRemoval implements AutoCloseable {
		final Raft leader;
		final long index;
		private final ArrayList<Raft> followers = new ArrayList<>();

		PendingRemoval(SmRaftTestCluster cluster, SmRaftTestCluster.Peer peer, BServiceInfo info) throws Exception {
			leader = cluster.nodes().stream().map(Rocks::getRaft).map(java.util.Objects::requireNonNull)
					.filter(Raft::isReadyLeader).findFirst().orElseThrow();
			try {
				for (var node : cluster.nodes()) {
					var raft = requireNonNull(node.getRaft());
					if (raft != leader) {
						raft.lock();
						followers.add(raft);
					}
				}
				var remove = new Edit();
				remove.Argument.getRemove().add(info);
				Assertions.assertEquals(Procedure.RaftRetry, peer.call(remove), "多数派被阻塞时删除应超时返回RaftRetry");
				leader.lock();
				try {
					index = leader.getLogSequence().getLastIndex();
				} finally {
					leader.unlock();
				}
				assertPending();
			} catch (Throwable e) {
				close();
				throw e;
			}
		}

		void assertPending() {
			leader.lock();
			try {
				Assertions.assertTrue(leader.isReadyLeader(), "故障窗口中原Leader仍应在任");
				Assertions.assertEquals(index, leader.getLogSequence().getLastIndex(), "不得追加依赖旧基线的业务日志");
				Assertions.assertTrue(leader.getLogSequence().getLastApplied() < index, "前序删除必须仍未应用");
			} finally {
				leader.unlock();
			}
		}

		@Override
		public void close() {
			for (int i = followers.size() - 1; i >= 0; --i)
				followers.get(i).unlock();
			followers.clear();
		}
	}

	private static void awaitReplicas(SmRaftTestCluster cluster, @Nullable BServiceInfo expected) throws Exception {
		await(() -> {
			long commonIndex = -1;
			for (var node : cluster.nodes()) {
				var raft = requireNonNull(node.getRaft());
				raft.lock();
				try {
					Assertions.assertFalse(raft.isShutdown, "恢复后节点必须保持运行");
					var sequence = raft.getLogSequence();
					if (sequence.getLastApplied() != sequence.getLastIndex())
						return false;
					if (commonIndex >= 0 && commonIndex != sequence.getLastIndex())
						return false;
					commonIndex = sequence.getLastIndex();
				} finally {
					raft.unlock();
				}
			}
			return replicasContain(cluster, expected);
		}, 60_000, "三个节点必须追平日志，且版本桶与注册内容一致");
	}

	private static boolean replicasContain(SmRaftTestCluster cluster, @Nullable BServiceInfo expected) throws Exception {
		for (var node : cluster.nodes()) {
			var table = requireNonNull(node.<String, BServerState>getTableTemplate("tServerState")).openTable();
			var matches = new boolean[]{expected == null};
			table.walk((name, state) -> {
				if (SERVICE.equals(name)) {
					var bucket = state.getServiceInfosVersion().get(VERSION);
					matches[0] = expected == null ? bucket == null
							: bucket != null && bucket.getServiceInfos().size() == 1
							&& bucket.getServiceInfos().get(expected.getServiceIdentity()) != null;
				}
				return true;
			});
			if (!matches[0])
				return false;
		}
		return true;
	}

	private static void await(Callable<Boolean> condition, long timeoutMs, String message) throws Exception {
		long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
		while (!condition.call()) {
			Assertions.assertTrue(System.nanoTime() < deadline, message);
			Thread.sleep(50);
		}
	}
}
