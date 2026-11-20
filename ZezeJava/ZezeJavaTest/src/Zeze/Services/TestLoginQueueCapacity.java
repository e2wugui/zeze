package Zeze.Services;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestLoginQueueCapacity {
	@Test
	void fullQueueRejectsWhileAllocationIsLockedAndReusesDrainedCapacity() throws Exception {
		Task.tryInitThreadPool();
		var config = new Config();
		var serviceConf = new ServiceConf();
		var limit = ServiceConf.class.getDeclaredField("maxConnections");
		limit.setAccessible(true);
		limit.setInt(serviceConf, 1);
		config.getServiceConfMap().put("LoginQueueCapacityTest", serviceConf);
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

	private static FutureTask<Boolean> acceptAfter(LoginQueue queue, Service service, CountDownLatch start) {
		var task = new FutureTask<>(() -> {
			start.await();
			return queue.tryOnAccept(new Socket(service));
		});
		Thread.ofVirtual().start(task);
		return task;
	}

	private static final class Socket extends AsyncSocket {
		Socket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eServer;
		}

		@Override
		protected void doClose(@Nullable Throwable error, boolean gracefully) {
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
	}
}
