package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Raft.LogSequence;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Services.ServiceManager.UnSubscribe;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import harness.ForkedJvm;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

@Fast
@ForkedJvm
@ResourceLock("sm-session-cluster")
public class TestServiceManagerUnsubscribeOrdering {

	@Test
	@Timeout(150)
	void tcpRejectedUnsubscribeKeepsLocalSubscription() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		var directory = Files.createTempDirectory(Path.of("autokeys"), "unsubscribe-rejected-");
		try (var server = new ServiceManagerServer(null, 28611, new Config(), directory.toString());
			 var agent = new Agent(new Config())) {
			agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", 28611, false));
			agent.start();
			waitReadyWithRetry(agent);
			var state = agent.subscribeService(new BSubscribeInfo("SM.Unsubscribe.Rejected", 0));
			var field = ServiceManagerServer.class.getDeclaredField("server");
			field.setAccessible(true);
			var endpoint = (Service)field.get(server);
			var original = endpoint.findProtocolFactoryHandle(UnSubscribe.TypeId_);
			// 受控服务端拒绝走真实 TCP 应答，验证错误码不被“本地先删”或 future 包装改变。
			endpoint.getFactorys().put(UnSubscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
					UnSubscribe::new, r -> { r.SendResultCode(-99); return 0; },
					TransactionLevel.None, DispatchMode.Direct));
			try {
				assertRejectedSubscriptionSurvives(agent, state, -99);
			} finally {
				endpoint.getFactorys().put(UnSubscribe.TypeId_, original);
			}
		} finally {
			LogSequence.deleteDirectory(directory.toFile());
		}
	}

	@Test
	@Timeout(150)
	void raftRejectedUnsubscribeKeepsLocalSubscription() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var config = Config.load();
			var sm = requireNonNull(config.getServiceManagerConf());
			sm.setRaftXml(cluster.configFile.toString());
			sm.setSessionName("SM.Unsubscribe.RejectedAgent");
			try (var agent = new ServiceManagerAgentWithRaft(config)) {
				agent.start();
				waitReadyWithRetry(agent);
				var state = agent.subscribeService(new BSubscribeInfo("SM.Unsubscribe.Rejected", 0));
				// 真实同名新连接接管会话，旧连接退订由服务端正常拒绝。
				cluster.login(sm.getSessionName());
				assertRejectedSubscriptionSurvives(agent, state, ServiceManagerWithRaft.ErrorNotLogin);
			}
		}
	}

	/** WaitReady()固定5s超时：满负载下连接建立可能超时（soak-20261022-1139 round9实锤），
	 * connector内部持续重连——有界重开等待窗口即可恢复（CommitThenResponse.Peer.connect同族加固）。 */
	private static void waitReadyWithRetry(AbstractAgent agent) throws InterruptedException {
		for (int attempt = 1; ; ++attempt) {
			try {
				agent.waitReady();
				return;
			} catch (Exception e) { // 超时经Task.forceThrow sneaky-throw受检TimeoutException，编译期不可见
				if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
					throw e;
				//noinspection BusyWait
				Thread.sleep(500);
			}
		}
	}

	private static void assertRejectedSubscriptionSurvives(AbstractAgent agent,
			AbstractAgent.SubscribeState state, long code) {
		var arg = new BUnSubscribeArgument();
		arg.serviceNames.add(state.getServiceName());
		var error = Assertions.assertThrows(IllegalStateException.class, () -> agent.unSubscribeService(arg));
		Assertions.assertTrue(error.getMessage().contains(Long.toString(code)));
		Assertions.assertSame(state, agent.getSubscribeStates().get(state.getServiceName()));
	}

	@Test
	@Timeout(150)
	void tcpResubscribeSurvivesEarlierUnsubscribeCaller() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		// SM 的兼容目录参数会补相对前缀；使用工作目录下唯一的相对目录。
		var directory = Files.createTempDirectory(Path.of("autokeys"), "unsubscribe-order-");
		// 专用端口；无 Application，不使用共享 DatabaseMemory/serverId 缓存目录。
		try (var server = new ServiceManagerServer(null, 28611, new Config(),
				directory.resolve("autokeys").toString()); var agent = new Agent(new Config())) {
			agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", 28611, false));
			agent.start();
			waitReadyWithRetry(agent);
			assertResubscribeSurvives(agent);
		} finally {
			LogSequence.deleteDirectory(directory.toFile());
		}
	}

	@Test
	@Timeout(150)
	void raftResubscribeSurvivesEarlierUnsubscribeCaller() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var config = Config.load();
			var sm = requireNonNull(config.getServiceManagerConf());
			sm.setRaftXml(cluster.configFile.toString());
			sm.setSessionName("SM.Unsubscribe.Ordering");
			try (var agent = new ServiceManagerAgentWithRaft(config)) {
				agent.start();
				waitReadyWithRetry(agent);
				assertResubscribeSurvives(agent);
			}
		}
	}

	private static void assertResubscribeSurvives(AbstractAgent agent) throws Exception {
		var service = "SM.Unsubscribe.Feed";
		agent.subscribeService(new BSubscribeInfo(service, 0));
		var arg = new PausedUnsubscribe(service);
		var failure = new AtomicReference<Throwable>();
		var caller = new Thread(() -> {
			try {
				agent.unSubscribeService(arg);
			} catch (Throwable e) {
				failure.set(e);
			} finally {
				// 修复后 IO 回调已先完成本地退订，调用者直接返回，不再遍历该列表。
				arg.names.ready.countDown();
			}
		}, "SM-unsubscribe-caller");
		arg.names.caller = caller;
		caller.start();
		AbstractAgent.SubscribeState replacement;
		try {
			Assertions.assertTrue(arg.names.ready.await(30, TimeUnit.SECONDS), "unsubscribe response arrived");
			Assertions.assertNull(failure.get(), "unsubscribe must succeed");
			// 旧实现：A 已收到成功应答，暂停在本地 remove 之前；B 的真实订阅应答先更新本地。
			// 新实现：A 在 IO 回调完成本地 remove 后才返回，B 随后订阅。
			replacement = agent.subscribeService(new BSubscribeInfo(service, 0));
		} finally {
			arg.names.release.countDown();
			caller.join(15_000); // 无论红绿都释放门闩并等待真实线程退出。
		}
		Assertions.assertFalse(caller.isAlive(), "unsubscribe caller must be drained");
		Assertions.assertNull(failure.get());
		var info = new BServiceInfo(service, "31", 0, "127.0.0.1", 7731);
		agent.registerService(info); // 服务端必须继续向 B 订阅发送真实增量。
		long deadline = System.nanoTime() + 5_000_000_000L;
		while (!contains(replacement, info) && System.nanoTime() < deadline)
			Thread.sleep(10);
		Assertions.assertAll(
				() -> Assertions.assertSame(replacement, agent.getSubscribeStates().get(service),
						"earlier unsubscribe caller must not remove the completed resubscription"),
				() -> Assertions.assertTrue(contains(replacement, info),
						"resubscribed route must receive later real Edit notifications"));
	}

	private static boolean contains(AbstractAgent.SubscribeState state, BServiceInfo info) {
		var bucket = state.getServiceInfos(0);
		return bucket != null && bucket.findServiceInfoByIdentity(info.getServiceIdentity()) != null;
	}

	/** 保留生产 wire 编码；仅在旧实现的“调用者应答后遍历”处暂停，绝不阻塞 IO 回调。 */
	private static final class PausedUnsubscribe extends BUnSubscribeArgument {
		final PausedNames names = new PausedNames();

		PausedUnsubscribe(String name) throws Exception {
			names.add(name);
			var field = BUnSubscribeArgument.class.getDeclaredField("serviceNames");
			field.setAccessible(true);
			field.set(this, names);
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteUInt(names.size());
			for (int i = 0; i < names.size(); ++i)
				bb.WriteString(names.get(i));
		}

		@Override
		public String toString() {
			return "PausedUnsubscribe"; // 日志格式化不能提前消费门闩。
		}
	}

	private static final class PausedNames extends ArrayList<String> {
		final CountDownLatch ready = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicBoolean paused = new AtomicBoolean();
		@Nullable Thread caller;

		@Override
		public Iterator<String> iterator() {
			if (Thread.currentThread() == caller && paused.compareAndSet(false, true)) {
				ready.countDown();
				try {
					if (!release.await(60, TimeUnit.SECONDS))
						throw new IllegalStateException("unsubscribe gate timeout");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
			}
			return super.iterator();
		}
	}
}
