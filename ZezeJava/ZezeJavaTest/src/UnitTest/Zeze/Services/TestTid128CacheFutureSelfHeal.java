package UnitTest.Zeze.Services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Component.Threading;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Services.ServiceManager.Id128UdpClient;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManager.Tid128Cache;
import Zeze.Util.Id128;
import harness.Fast;
import org.jetbrains.annotations.NotNull;

/**
 * S2-1：allocateTid128CacheFuture 遇到异常完成（毒化）的 lastTid128CacheFuture 时必须自愈替换。
 * <p>
 * 原实现先 future.get() 再替换（唯一写入点）：上一次分配Udp超时后get()抛CompletionException，
 * 替换永不执行，毒化状态永久保留——冷写事务（_check_预热）持续失败；热记录事务不经_check_直达
 * finalCommit，getLastTid128CacheFuture返回毒化future，buildLogChanges内get()抛异常导致halt(543543)。
 * <p>
 * 真实Udp超时需5秒，不可取。这里用真实loopback Id128UdpServer保证udp.send总是成功（future保持pending），
 * 但不启动Id128UdpClient工作线程（应答无人处理），future的完成状态完全由测试手动setResult/setException
 * 控制，毒化/正常完成皆可确定性构造。
 */
@Fast
public class TestTid128CacheFutureSelfHeal {
	private static final String globalName = "UnitTest.S2.Tid128CacheFutureSelfHeal";
	private static Id128UdpServer server;
	private static TestAgent agent;

	@BeforeAll
	public static void setUp() throws Exception {
		server = new Id128UdpServer();
		server.start();
		agent = new TestAgent();
		agent.setTid128UdpClient(new Id128UdpClient(agent, "127.0.0.1", server.getLocalPort(),
				new AtomicLong()::incrementAndGet)); // 不调用start()：工作线程不启动，future保持pending。
	}

	@AfterAll
	public static void tearDown() throws Exception {
		agent.client.stop();
		server.stop();
	}

	// 不能把pending的future留给下一个测试：allocateTid128CacheFuture内部会get()等待上一次分配完成。
	@AfterEach
	public void completeLastFutureIfPending() {
		var last = agent.getLastTid128CacheFuture();
		if (last != null && !last.isDone())
			Assertions.assertTrue(last.setException(new TimeoutException("test cleanup")));
	}

	@Test
	public void testPoisonedLastFutureSelfHeals() {
		var f1 = agent.allocateTid128CacheFuture(globalName);
		Assertions.assertTrue(f1.setException(new TimeoutException("simulate udp timeout")));
		Assertions.assertTrue(f1.isCompletedExceptionally());

		// 修复前：这里抛CompletionException，且lastTid128CacheFuture永不被替换（毒化永久保留）。
		var f2 = Assertions.assertDoesNotThrow(() -> agent.allocateTid128CacheFuture(globalName));
		Assertions.assertNotSame(f1, f2);
		Assertions.assertSame(f2, agent.getLastTid128CacheFuture());

		// 自愈后的新分配可用（模拟SM恢复后正常应答）。
		Assertions.assertTrue(f2.setResult(new Tid128Cache(globalName, agent, new Id128(0, 0), 16)));
		Assertions.assertEquals(new Id128(0, 1), f2.get().next());
	}

	@Test
	public void testPoisonSelfHealRepeatable() {
		for (var i = 0; i < 3; i++) {
			var poisoned = agent.allocateTid128CacheFuture(globalName);
			Assertions.assertTrue(poisoned.setException(new TimeoutException("simulate udp timeout " + i)));
			var next = Assertions.assertDoesNotThrow(() -> agent.allocateTid128CacheFuture(globalName));
			Assertions.assertNotSame(poisoned, next);
			Assertions.assertSame(next, agent.getLastTid128CacheFuture());
			Assertions.assertTrue(next.setException(new TimeoutException("loop " + i))); // 保持last完成，避免下一轮get()阻塞。
		}
	}

	@Test
	public void testNormalCompletedLastFutureStillAdaptive() {
		var f1 = agent.allocateTid128CacheFuture(globalName);
		Assertions.assertTrue(f1.setResult(new Tid128Cache(globalName, agent, new Id128(0, 0), Tid128Cache.ALLOCATE_COUNT_MAX)));

		// 正常完成的last future走原自适应档位路径，不受毒化检测影响。
		var f2 = Assertions.assertDoesNotThrow(() -> agent.allocateTid128CacheFuture(globalName));
		Assertions.assertNotSame(f1, f2);
		Assertions.assertSame(f2, agent.getLastTid128CacheFuture());
	}

	@Test
	public void testGetUsableTid128CacheFuture() {
		// 毒化时兜底替换（finalCommit读取路径的决策）。
		var f1 = agent.allocateTid128CacheFuture(globalName);
		Assertions.assertTrue(f1.setException(new TimeoutException("simulate udp timeout")));
		var usable = agent.getUsableTid128CacheFuture(globalName);
		Assertions.assertNotSame(f1, usable);
		Assertions.assertFalse(usable.isCompletedExceptionally());
		Assertions.assertSame(usable, agent.getLastTid128CacheFuture());

		// 未毒化（pending或正常完成）时原样返回，不产生多余分配。
		Assertions.assertSame(usable, agent.getUsableTid128CacheFuture(globalName));
		Assertions.assertTrue(usable.setResult(new Tid128Cache(globalName, agent, new Id128(0, 0), Tid128Cache.ALLOCATE_COUNT_MIN)));
		Assertions.assertSame(usable, agent.getUsableTid128CacheFuture(globalName));
	}

	/** 只实现被测路径需要的行为，其余入口不可用。 */
	private static final class TestAgent extends AbstractAgent {
		private Id128UdpClient client;

		void setTid128UdpClient(Id128UdpClient client) {
			this.client = client;
			this.tid128UdpClient = client; // protected字段，子类内可访问。
		}

		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void waitReady() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void editService(@NotNull BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(@NotNull BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}
	}
}
