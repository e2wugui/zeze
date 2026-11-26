package Zeze.Services;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.LoginQueue.PutLoginToken;
import Zeze.Builtin.LoginQueue.PutQueueFull;
import Zeze.Builtin.LoginQueue.PutQueuePosition;
import Zeze.Builtin.LoginQueueServer.BServerLoad;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestLoginQueueCapacity {
	@Test
	void fullQueueRejectsWhileAllocationIsLockedAndReusesDrainedCapacity() throws Exception {
		Task.tryInitThreadPool();
		var config = newConfigWithMaxConnections("LoginQueueCapacityTest", 1);
		var queue = new LoginQueue(config);
		var service = new Service("LoginQueueCapacityTest", config);
		try {
			var pending = new Socket(service);
			assertTrue(queue.tryOnAccept(pending));
			var field = LoginQueue.class.getDeclaredField("allocateLock");
			field.setAccessible(true);
			var lock = (ReentrantLock)field.get(queue);
			var rejected = new Socket(service);
			var full = new FutureTask<>(() -> queue.tryOnAccept(rejected));
			lock.lock();
			try {
				Thread.ofVirtual().start(full);
				assertFalse(full.get(5, TimeUnit.SECONDS));
				assertTrue(rejected.isClosed());
			} finally {
				lock.unlock();
			}
			pending.close();
			queue.drainQueue();
			var start = new CountDownLatch(1);
			var first = acceptAfter(queue, service, start);
			var second = acceptAfter(queue, service, start);
			start.countDown();
			assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
		} finally {
			queue.stop();
		}
	}

	@Test
	void middleDisconnectsReleaseSlotsImmediatelyWhileHeadStaysAlive() throws Exception {
		Task.tryInitThreadPool();
		var config = newConfigWithMaxConnections("LoginQueueMiddleDisconnectTest", 3);
		var queue = new LoginQueue(config);
		var service = new Service("LoginQueueMiddleDisconnectTest", config);
		try {
			var head = new Socket(service, queue);
			var m1 = new Socket(service, queue);
			var m2 = new Socket(service, queue);
			assertTrue(queue.tryOnAccept(head));
			assertTrue(queue.tryOnAccept(m1));
			assertTrue(queue.tryOnAccept(m2));
			assertEquals(3, queue.getQueuedCount());

			// 3 个全部存活时，第 4 个收到 PutQueueFull
			var fullRejected = new Socket(service, queue);
			assertFalse(queue.tryOnAccept(fullRejected));
			assertTrue(fullRejected.hasSent(PutQueueFull.class));
			assertTrue(fullRejected.isClosed());

			// 队头存活且无可用后端（drainQueue 无法移除队头），中部反复断连重连：
			// onClose O(1) 实时释放槽位，新连接不得误收 PutQueueFull
			var current1 = m1;
			var current2 = m2;
			for (var round = 0; round < 20; round++) {
				current1.close();
				current2.close();
				assertEquals(1, queue.getQueuedCount(), "中部断连后应立即可见仅剩队头 1 个槽位");

				var next1 = new Socket(service, queue);
				var next2 = new Socket(service, queue);
				assertTrue(queue.tryOnAccept(next1), "round " + round + " next1 不得误发 PutQueueFull");
				assertTrue(queue.tryOnAccept(next2), "round " + round + " next2 不得误发 PutQueueFull");
				assertFalse(next1.hasSent(PutQueueFull.class));
				assertFalse(next2.hasSent(PutQueueFull.class));
				assertEquals(3, queue.getQueuedCount());

				current1 = next1;
				current2 = next2;
			}

			// 即使调用 drainQueue，队头存活时计数仍为 3；满额后再来新连接仍应拒绝
			queue.drainQueue();
			assertEquals(3, queue.getQueuedCount());
			var stillFull = new Socket(service, queue);
			assertFalse(queue.tryOnAccept(stillFull));
			assertTrue(stillFull.hasSent(PutQueueFull.class));

			// 队头关闭后立刻释放 1 个槽位，新连接立即可接入
			head.close();
			assertEquals(2, queue.getQueuedCount());
			var afterHeadClosed = new Socket(service, queue);
			assertTrue(queue.tryOnAccept(afterHeadClosed));
			assertEquals(3, queue.getQueuedCount());
		} finally {
			queue.stop();
		}
	}

	@Test
	void allocateTimerSweepsMiddleZombiesAndBroadcastsAccuratePositions() throws Exception {
		Task.tryInitThreadPool();
		var config = newConfigWithMaxConnections("LoginQueueBroadcastSweepTest", 10);
		var queue = new LoginQueue(config);
		var service = new Service("LoginQueueBroadcastSweepTest", config);
		try {
			var c1 = new Socket(service, queue);
			var z1 = new Socket(service, queue);
			var z2 = new Socket(service, queue);
			var c2 = new Socket(service, queue);
			// z3 不回调 onClose，验证周期遍历顺带清理也能兜底释放其槽位且不重复扣减
			var z3 = new Socket(service);
			var c3 = new Socket(service, queue);

			for (var s : List.of(c1, z1, z2, c2, z3, c3))
				assertTrue(queue.tryOnAccept(s));
			assertEquals(6, queue.getQueuedCount());

			z1.close();
			z2.close();
			z3.close();
			assertEquals(4, queue.getQueuedCount(), "z1/z2 经 onClose 实时扣减，z3 尚未触发 onClose");
			assertEquals(6, internalQueueOf(queue).size(), "广播遍历前中部僵尸仍在队列中");

			// 连续 3 次 allocateTimer 触发广播与中部僵尸清理
			queue.allocateTimer();
			queue.allocateTimer();
			assertTrue(c1.positions().isEmpty(), "前 2 次 tick 尚未到达广播周期");
			queue.allocateTimer();

			assertEquals(List.of(1), c1.positions());
			assertEquals(List.of(2), c2.positions(), "跳过中部僵尸后 c2 位置应为 2");
			assertEquals(List.of(3), c3.positions(), "跳过中部僵尸后 c3 位置应为 3");
			assertTrue(z1.positions().isEmpty());
			assertTrue(z2.positions().isEmpty());
			assertTrue(z3.positions().isEmpty());
			assertEquals(3, internalQueueOf(queue).size(), "中部僵尸节点应被 Iterator.remove() 清除");
			assertEquals(3, queue.getQueuedCount(), "z3 的兜底槽位也应被准确释放");

			// z3 迟到调用 onClose 不得二次扣减
			queue.onClose(z3);
			assertEquals(3, queue.getQueuedCount());
		} finally {
			queue.stop();
		}
	}

	@Test
	void concurrentOnCloseDrainQueueAndBroadcastNeverDoubleDecrementOrDropLiveSockets() throws Exception {
		Task.tryInitThreadPool();
		var config = newConfigWithMaxConnections("LoginQueueConcurrentDrainTest", 1000);
		var queue = new LoginQueue(config, 40, true);
		var service = new Service("LoginQueueConcurrentDrainTest", config);
		try {
			final int total = 300;
			var sockets = new ArrayList<Socket>(total);
			for (var i = 0; i < total; i++) {
				var s = new Socket(service, queue);
				assertTrue(queue.tryOnAccept(s));
				sockets.add(s);
			}
			assertEquals(total, queue.getQueuedCount());

			// 注册高容量 link 使 drainQueue 开始分配 PutLoginToken（并 closeGracefully -> onClose）
			registerMockLink(queue, new Socket(service), 10000);

			var start = new CountDownLatch(1);
			var done = new AtomicBoolean();
			var negativeObserved = new AtomicBoolean();

			// 线程 1：并发关闭所有奇数下标连接
			var closer = new FutureTask<Void>(() -> {
				start.await();
				for (var i = 1; i < total; i += 2) {
					sockets.get(i).close();
					if (queue.getQueuedCount() < 0)
						negativeObserved.set(true);
				}
				return null;
			});

			// 线程 2：并发调用 drainQueue 直到排空
			var drainer = new FutureTask<Void>(() -> {
				start.await();
				var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
				while ((queue.getQueuedCount() > 0 || !internalQueueOf(queue).isEmpty())
						&& System.nanoTime() < deadline) {
					queue.drainQueue();
					if (queue.getQueuedCount() < 0)
						negativeObserved.set(true);
				}
				return null;
			});

			// 线程 3：并发执行 broadcastQueuePositions（含 Iterator.remove()），压测与 drainQueue 并发竞态
			var broadcaster = new FutureTask<Void>(() -> {
				start.await();
				while (!done.get()) {
					queue.broadcastQueuePositions();
					if (queue.getQueuedCount() < 0)
						negativeObserved.set(true);
				}
				return null;
			});

			Thread.ofVirtual().start(closer);
			Thread.ofVirtual().start(drainer);
			Thread.ofVirtual().start(broadcaster);
			start.countDown();

			closer.get(10, TimeUnit.SECONDS);
			drainer.get(10, TimeUnit.SECONDS);
			done.set(true);
			broadcaster.get(10, TimeUnit.SECONDS);

			assertFalse(negativeObserved.get(), "queuedCount 绝不可出现负值");
			assertEquals(0, queue.getQueuedCount(), "全部排空后 queuedCount 必须为 0");
			assertTrue(internalQueueOf(queue).isEmpty(), "全部排空后内部队列必须为空");

			// 所有未被主动关闭的偶数连接都必须恰好收到 1 次 PutLoginToken（验证无活连接被错删）
			for (var i = 0; i < total; i += 2) {
				var s = sockets.get(i);
				assertEquals(1, s.countSent(PutLoginToken.class),
						"活连接 #" + i + " 必须恰好收到 1 次 PutLoginToken");
			}
			// 被关闭的奇数连接至多收到 1 次（可能在关闭前已被分配），绝不可重复分配
			for (var i = 1; i < total; i += 2) {
				var s = sockets.get(i);
				assertTrue(s.countSent(PutLoginToken.class) <= 1,
						"连接 #" + i + " 绝不可收到重复 PutLoginToken");
			}
		} finally {
			queue.stop();
		}
	}

	@Test
	void acceptAndCloseRaceDoesNotLeakQueuedCount() throws Exception {
		Task.tryInitThreadPool();
		var config = newConfigWithMaxConnections("LoginQueueAcceptCloseRaceTest", 4096);
		var queue = new LoginQueue(config);
		var service = new Service("LoginQueueAcceptCloseRaceTest", config);
		try {
			// 已关闭连接调用 tryOnAccept：直接返回 false，不入队不发 PutQueueFull 不泄漏计数
			var alreadyClosed = new Socket(service, queue);
			alreadyClosed.close();
			assertFalse(queue.tryOnAccept(alreadyClosed));
			assertEquals(0, queue.getQueuedCount());
			assertTrue(internalQueueOf(queue).isEmpty());
			assertTrue(alreadyClosed.sent.isEmpty());

			final int iterations = 1500;
			var accepted = new AtomicInteger();
			var negativeObserved = new AtomicBoolean();
			var allSockets = new ArrayList<Socket>(iterations);

			for (var i = 0; i < iterations; i++) {
				var s = new Socket(service, queue);
				allSockets.add(s);
				var go = new CountDownLatch(1);
				var acceptTask = new FutureTask<>(() -> {
					go.await();
					if (queue.tryOnAccept(s))
						accepted.incrementAndGet();
					if (queue.getQueuedCount() < 0)
						negativeObserved.set(true);
					return null;
				});
				var closeTask = new FutureTask<Void>(() -> {
					go.await();
					s.close();
					if (queue.getQueuedCount() < 0)
						negativeObserved.set(true);
					return null;
				});
				Thread.ofVirtual().start(acceptTask);
				Thread.ofVirtual().start(closeTask);
				go.countDown();
				acceptTask.get(5, TimeUnit.SECONDS);
				closeTask.get(5, TimeUnit.SECONDS);
			}

			assertFalse(negativeObserved.get(), "竞态期间 queuedCount 绝不可为负");
			assertEquals(0, queue.getQueuedCount(),
					"所有连接均已关闭，Dekker 双检必须保证 queuedCount 归零无泄漏 (accepted=" + accepted.get() + ")");

			// 清理可能残留在队列里的已关闭节点，计数仍须保持 0
			queue.broadcastQueuePositions();
			queue.drainQueue();
			assertEquals(0, queue.getQueuedCount());
			assertTrue(internalQueueOf(queue).isEmpty());
			for (var s : allSockets)
				assertTrue(s.isClosed());
		} finally {
			queue.stop();
		}
	}

	@Test
	void stopClearsRemainingQueueAndPreventsPostStopUnderflow() throws Exception {
		Task.tryInitThreadPool();
		var config = newConfigWithMaxConnections("LoginQueueStopClearTest", 16);
		var queue = new LoginQueue(config);
		var service = new Service("LoginQueueStopClearTest", config);
		var sockets = new ArrayList<Socket>(5);
		try {
			for (var i = 0; i < 5; i++) {
				var s = new Socket(service, queue);
				assertTrue(queue.tryOnAccept(s));
				sockets.add(s);
			}
			assertEquals(5, queue.getQueuedCount());
			queue.stop();
			assertEquals(0, queue.getQueuedCount());
			assertTrue(internalQueueOf(queue).isEmpty());

			// stop 后迟到断连/onClose 不得令 queuedCount 变为负数
			for (var s : sockets)
				s.close();
			assertEquals(0, queue.getQueuedCount());
		} finally {
			queue.stop();
		}
	}

	private static Config newConfigWithMaxConnections(String serviceName, int maxConnections) throws Exception {
		var config = new Config();
		var serviceConf = new ServiceConf();
		var limit = ServiceConf.class.getDeclaredField("maxConnections");
		limit.setAccessible(true);
		limit.setInt(serviceConf, maxConnections);
		config.getServiceConfMap().put(serviceName, serviceConf);
		return config;
	}

	private static Collection<?> internalQueueOf(LoginQueue queue) throws Exception {
		var field = LoginQueue.class.getDeclaredField("queue");
		field.setAccessible(true);
		return (Collection<?>)field.get(queue);
	}

	@SuppressWarnings("unchecked")
	private static void registerMockLink(LoginQueue queue, AsyncSocket linkSocket, int capacity) throws Exception {
		var serverField = LoginQueue.class.getDeclaredField("server");
		serverField.setAccessible(true);
		var server = (LoginQueueServer)serverField.get(queue);
		var linksField = LoginQueueServer.class.getDeclaredField("links");
		linksField.setAccessible(true);
		var links = (ConcurrentHashMap<AsyncSocket, BServerLoad.Data>)linksField.get(server);

		var load = new BLoad.Data();
		load.setProposeMaxOnline(capacity);
		load.setMaxOnlineNew(capacity);
		var serverLoad = new BServerLoad.Data();
		serverLoad.setServerId(1);
		serverLoad.setServiceIp("127.0.0.1");
		serverLoad.setServicePort(9000);
		serverLoad.setLoad(load);
		links.put(linkSocket, serverLoad);
	}

	private static FutureTask<Boolean> acceptAfter(LoginQueue queue, Service service, CountDownLatch start) {
		var task = new FutureTask<>(() -> {
			start.await();
			return queue.tryOnAccept(new Socket(service));
		});
		Thread.ofVirtual().start(task);
		return task;
	}

	private static final class Socket extends AsyncSocket {
		private final @Nullable LoginQueue closeTarget;
		final CopyOnWriteArrayList<Protocol<?>> sent = new CopyOnWriteArrayList<>();

		Socket(Service service) {
			this(service, null);
		}

		Socket(Service service, @Nullable LoginQueue closeTarget) {
			super(service);
			this.closeTarget = closeTarget;
		}

		@Override
		public Type getType() {
			return Type.eServer;
		}

		@Override
		protected void doClose(@Nullable Throwable error, boolean gracefully) {
			if (closeTarget != null)
				closeTarget.onClose(this);
		}

		@Override
		public boolean Send(Protocol<?> p) {
			sent.add(p);
			return true;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		boolean hasSent(Class<? extends Protocol<?>> type) {
			return countSent(type) > 0;
		}

		int countSent(Class<? extends Protocol<?>> type) {
			var count = 0;
			for (var p : sent) {
				if (type.isInstance(p))
					count++;
			}
			return count;
		}

		List<Integer> positions() {
			var result = new ArrayList<Integer>();
			for (var p : sent) {
				if (p instanceof PutQueuePosition pos)
					result.add(pos.Argument.getQueuePosition());
			}
			return result;
		}
	}
}
