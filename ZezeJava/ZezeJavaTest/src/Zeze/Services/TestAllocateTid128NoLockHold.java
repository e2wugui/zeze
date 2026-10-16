package Zeze.Services;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Component.Threading;
import Zeze.Config;
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
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND3-39衍生：allocateTid128CacheFuture锁内的future.get()虽然不死锁（完成在UDP
 * 接收线程、不经agent锁），但等待未完成future会把agent锁占住剩余RTT——丢包时要
 * 等重传超时（eRpcTimeout=5s，由1.5s周期的检查器毒化）才跳出，期间其他分配调用者
 * 全被卡。与32位路径统一为锁内非阻塞探测：未完成退用默认档位立即分配。
 * 测试手法同TestTid128CacheFuturePerName：client不start()，future永久pending，
 * 确定性构造"在途"窗口。
 */
@Fast
public class TestAllocateTid128NoLockHold {
	private static final String NAME = "UnitTest.Tid128NoLockHold";
	private static @Nullable Id128UdpServer server;
	private static @Nullable TestAgent agent;

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
		requireNonNull(requireNonNull(agent).client).stop();
		requireNonNull(server).stop();
	}

	// 不能把pending的future留给下一个测试（毒化释放可能的等待者）。
	@AfterEach
	public void completePendingFutures() {
		var last = requireNonNull(agent).getLastTid128CacheFuture(NAME);
		if (last != null && !last.isDone())
			Assertions.assertTrue(last.setException(new TimeoutException("test cleanup")));
	}

	@Test
	public void testSecondAllocateNotBlockOnPendingFuture() throws Exception {
		var f1 = requireNonNull(agent).allocateTid128CacheFuture(NAME, 200); // pending：工作线程未启动
		Assertions.assertFalse(f1.isDone(), "F1应在途（永久pending）");

		// 上一个future在途时，第二个线程分配不得持锁死等。
		// 未修复：锁内F1.get()占住agent锁直到F1异常完成（无工作线程=永不），调用者全部被卡。
		ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
			var t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		try {
			var f2Alloc = pool.submit(() -> requireNonNull(agent).allocateTid128CacheFuture(NAME, 0));
			var f2 = Assertions.assertDoesNotThrow(() -> f2Alloc.get(2, TimeUnit.SECONDS),
					"在途future上的并发分配不得持锁死等（FND3-39衍生）");
			Assertions.assertNotSame(f1, f2);
		} finally {
			pool.shutdownNow();
			// 旧代码卡死时毒化F1放行被卡线程（daemon之外的保险）。
			Assertions.assertTrue(f1.setException(new TimeoutException("unwedge")));
		}
	}

	/** 只实现被测路径（同TestTid128CacheFuturePerName的桩）。 */
	private static final class TestAgent extends AbstractAgent {
		@Nullable Id128UdpClient client;

		private TestAgent() {
			config = new Config();
		}

		void setTid128UdpClient(Id128UdpClient client) {
			this.client = client;
			this.tid128UdpClient = client; // protected字段，子类内可访问。
		}

		@Override
		protected void allocate(AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(String globalName, int allocCount,
										ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() {
		}

		@Override
		public void waitReady() {
		}

		@Override
		public void editService(BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<List<SubscribeState>> subscribeServicesAsync(
				BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
		}
	}
}
