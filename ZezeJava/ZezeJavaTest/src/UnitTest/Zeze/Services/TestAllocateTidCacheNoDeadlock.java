package UnitTest.Zeze.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AllocateId;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Services.ServiceManager.TidCache;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-39：allocateTidCacheFuture在agent锁内无超时get()上一个未完成的future，
 * 而该future的完成回调第一步是同一把锁——上一分配在途时第二个线程进来分配，
 * 互相等待：死锁且冻结IO/回调线程。
 * 修复：锁内只做非阻塞探测（isDone/getNow），未完成用默认档位立即分配。
 */
@Fast
public class TestAllocateTidCacheNoDeadlock {

	/** 只实现被测路径：allocateAsync捕获回调不自动完成（模拟RPC在途）。 */
	private static final class StubAgent extends AbstractAgent {
		final List<ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>>> callbacks = new ArrayList<>();
		final List<Integer> allocCounts = new ArrayList<>();

		private StubAgent() {
			config = new Config();
		}

		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			allocCounts.add(allocCount);
			callbacks.add(callback); // 应答未到达：future在途
			return true;
		}

		@Override
		public void start() {
		}

		@Override
		public void waitReady() {
		}

		@Override
		public void editService(@NotNull BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(
				@NotNull BSubscribeArgument info) {
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
		}
	}

	/** 模拟第index次分配的RPC应答到达：调用捕获的回调完成对应future。 */
	private static void completeRpc(StubAgent agent, int index, long startId, int count) throws Exception {
		var r = new AllocateId();
		r.setResultCode(0);
		r.Result.setStartId(startId);
		r.Result.setCount(count);
		Assertions.assertEquals(0L, agent.callbacks.get(index).handle(r));
	}

	@Test
	public void testSecondAllocateNotDeadlockOnInflightFuture() throws Exception {
		var agent = new StubAgent();
		var f1 = agent.allocateTidCacheFuture("g");
		Assertions.assertFalse(f1.isDone(), "F1应在途（回调未完成）");

		// 上一个future在途时，第二个线程分配不得持锁死等（未修复：F1.get()持锁死等，
		// 而F1的完成回调需要同一把锁，互相等待永不返回）。daemon线程：旧代码卡死时测试仍可结束。
		ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
			var t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		try {
			var f2Alloc = pool.submit(() -> agent.allocateTidCacheFuture("g"));
			var f2 = Assertions.assertDoesNotThrow(() -> f2Alloc.get(2, TimeUnit.SECONDS),
					"在途future上的并发分配不得持锁死等（FND3-39）");
			Assertions.assertNotSame(f1, f2);
			// 在途窗口退用默认档位（档位仅是优化参数，号段允许并发分配）。
			Assertions.assertEquals(List.of(TidCache.ALLOCATE_COUNT_MIN, TidCache.ALLOCATE_COUNT_MIN),
					agent.allocCounts, "在途窗口应使用默认档位");
		} finally {
			pool.shutdownNow();
		}

		// F1的完成回调需要agent锁：不得被残留阻塞。
		Assertions.assertDoesNotThrow(() -> completeRpc(agent, 0, 100, 200));
		var cache = f1.get(1, TimeUnit.SECONDS);
		Assertions.assertEquals(100L, cache.getStart());
	}

	@Test
	public void testAdaptiveCountPreserved() throws Exception {
		var agent = new StubAgent();
		// 第一次：无历史，默认最小档。
		agent.allocateTidCacheFuture("g");
		Assertions.assertEquals(List.of(TidCache.ALLOCATE_COUNT_MIN), agent.allocCounts);

		// 完成：start=0,count=200 → allocateCount()=max(16, half=100)=100。
		completeRpc(agent, 0, 0, 200);
		// 第二次：上次已完成 → 档位自适应仍生效。
		agent.allocateTidCacheFuture("g");
		Assertions.assertEquals(100, agent.allocCounts.get(1), "已完成的上次分配应继续提供自适应档位");
	}
}
