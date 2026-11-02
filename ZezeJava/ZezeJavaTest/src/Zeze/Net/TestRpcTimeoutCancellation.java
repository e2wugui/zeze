package Zeze.Net;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.ThreadDiagnosable;
import harness.Fast;
import harness.ForkedJvm;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
@ForkedJvm
public class TestRpcTimeoutCancellation {

	@AfterEach
	void cleanup() throws Exception {
		try {
			Task.shutdownNow(5_000);
		} finally {
			ThreadDiagnosable.stopDiagnose();
		}
	}

	@Test
	void initThreadPoolRemovesCancelledTasks() {
		var scheduler = new ScheduledThreadPoolExecutor(1);
		Task.initThreadPool(Executors.newSingleThreadExecutor(), scheduler);
		assertCancellationRemovesTask(scheduler);
	}

	@Test
	void tryInitThreadPoolRemovesCancelledTasks() {
		var scheduler = new ScheduledThreadPoolExecutor(1);
		assertTrue(Task.tryInitThreadPool((Config)null, Executors.newSingleThreadExecutor(), scheduler));
		assertCancellationRemovesTask(scheduler);
	}

	@Test
	void defaultThreadPoolRemovesCancelledTasks() {
		assertTrue(Task.tryInitThreadPool());
		assertCancellationRemovesTask((ScheduledThreadPoolExecutor)Task.getScheduledThreadPool());
	}

	private static void assertCancellationRemovesTask(ScheduledThreadPoolExecutor scheduler) {
		var task = TaskSpec.ofAction(() -> fail("cancelled task must not run")).scheduleNow(60_000);
		assertTrue(scheduler.getQueue().contains(task));
		assertTrue(task.cancel(false));
		assertFalse(scheduler.getQueue().contains(task), "cancelled task must leave the queue immediately");
	}

	@Test
	void contextRemovedDuringRegistrationCancelsTimeout() throws Exception {
		var scheduler = new RegistrationScheduler();
		// 单独验证句柄发布竞态，不依赖初始化入口的策略修复。
		scheduler.setRemoveOnCancelPolicy(true);
		Task.initThreadPool(Executors.newSingleThreadExecutor(), scheduler);
		var service = new Service("TestRpcTimeoutCancellation.Registration");
		var rpc = new TestRpcTimeoutTimerLeak.TimerRpc();
		long sid = service.addRpcContext(rpc);
		rpc.setSessionId(sid);
		var registration = Task.getThreadPool().submit(() -> rpc.schedule(service, sid, 60_000));
		try {
			assertTrue(scheduler.queued.await(5, TimeUnit.SECONDS));
			assertSame(rpc, service.removeRpcContext(sid));
			scheduler.release.countDown();
			registration.get(5, TimeUnit.SECONDS);
			var task = Objects.requireNonNull(scheduler.registered);
			assertTrue(task.isCancelled(), "context consumed before handle publication must cancel timeout");
			assertFalse(scheduler.getQueue().contains(task));
		} finally {
			scheduler.release.countDown();
			registration.get(5, TimeUnit.SECONDS);
			service.stop();
		}
	}

	@Test
	void contextRemovedBeforeRegistrationCancelsTimeout() throws Exception {
		var scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setRemoveOnCancelPolicy(true);
		Task.initThreadPool(Executors.newSingleThreadExecutor(), scheduler);
		var service = new Service("TestRpcTimeoutCancellation.BeforeRegistration");
		try {
			var rpc = new TestRpcTimeoutTimerLeak.TimerRpc();
			long sid = service.addRpcContext(rpc);
			rpc.setSessionId(sid);
			assertTrue(service.removeRpcContext(sid, rpc));
			int baseline = scheduler.getQueue().size();
			rpc.schedule(service, sid, 60_000);
			assertEquals(baseline, scheduler.getQueue().size(), "consumed context must not retain a timeout");
		} finally {
			service.stop();
		}
	}

	private static final class RegistrationScheduler extends ScheduledThreadPoolExecutor {
		final CountDownLatch queued = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		volatile @Nullable ScheduledFuture<?> registered;

		RegistrationScheduler() {
			super(1);
		}

		@Override
		public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
			var task = super.schedule(callable, delay, unit);
			registered = task;
			queued.countDown();
			try {
				assertTrue(release.await(10, TimeUnit.SECONDS), "registration must be released");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			}
			return task;
		}
	}
}
