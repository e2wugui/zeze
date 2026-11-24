package Zeze.Component;

import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestThreadingTimeoutAfterKeepAlive {
	private static final int SERVER_ID = 947;
	private static final String MUTEX_NAME = "timeout-after-keepalive";

	@Test
	public void concurrentKeepAlivePreservesHeldMutex() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var server = new ThreadingServer(new Service("TestThreadingTimeoutAfterKeepAlive"),
				new ServiceManagerServer.Conf());
		try {
			var heartbeat = new KeepAlive();
			heartbeat.Argument.setServerId(SERVER_ID);
			heartbeat.Argument.setAppSerialId(1);
			assertEquals(0L, server.ProcessKeepAlive(heartbeat));
			assertEquals(0L, tryLock(server, 1));

			var ownersField = ThreadingServer.class.getDeclaredField("simulateThreadsByServerId");
			ownersField.setAccessible(true);
			var owner = ((Map<?, ?>)ownersField.get(server)).get(SERVER_ID);
			var activityField = owner.getClass().getDeclaredField("activeTime");
			activityField.setAccessible(true);
			var timeoutRelease = ThreadingServer.class.getDeclaredMethod("timeoutRelease");
			timeoutRelease.setAccessible(true);
			var scan = new FutureTask<>(() -> timeoutRelease.invoke(server));
			var scanThread = Thread.ofPlatform().daemon().unstarted(scan);

			var lock = (ReentrantLock)server.getLock();
			lock.lock();
			try {
				activityField.setLong(owner, 1L);
				scanThread.start();
				// The old path decides to release before it blocks on this lock.
				await(() -> lock.hasQueuedThread(scanThread));
				assertEquals(0L, server.ProcessKeepAlive(heartbeat));
			} finally {
				lock.unlock();
			}
			scan.get(5, TimeUnit.SECONDS);
			assertEquals(1L, tryLock(server, 2), "heartbeat refreshed the owner before the scan acquired the lock");
		} finally {
			server.close();
		}
	}

	private static long tryLock(ThreadingServer server, long threadId) throws Exception {
		var request = new MutexTryLock();
		request.Argument.setLockName(new BLockName(new BGlobalThreadId(SERVER_ID, threadId), MUTEX_NAME));
		request.Argument.setTimeoutMs(1000);
		assertEquals(0L, server.ProcessMutexTryLockRequest(request));
		await(request::isSendResultDone);
		return request.getResultCode();
	}

	private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean() && System.nanoTime() < deadline)
			Thread.sleep(1);
		assertTrue(condition.getAsBoolean(), "timed out waiting for the threading server");
	}
}
