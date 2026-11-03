package Zeze.Net;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import harness.ForkedJvm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
@ForkedJvm
public class TestRpcTimeoutCancellation {

	@AfterEach
	void cleanup() throws Exception {
		Task.shutdownNow(5_000);
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
		assertTrue(Task.tryInitThreadPool(null, Executors.newSingleThreadExecutor(), scheduler));
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

	/** 换代注（Rpc 超时上 TimeoutManager 轮子）：原版经伪造 ScheduledThreadPoolExecutor.schedule
	 * 内部阻塞复现"句柄发布前上下文被消费"，注册收窄为单次同步调用后注射点退役。
	 * 现以并发锤+终态断言守护同一契约：无论 schedule 与上下文消费怎样交错，两操作完成后
	 * 句柄必被取消收口（发布后复查路径或 removeRpcContext 收口路径，终点一致为 null）。 */
	@Test
	void contextRemovedDuringRegistrationCancelsTimeout() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestRpcTimeoutCancellation.Registration");
		var field = Rpc.class.getDeclaredField("timeoutTask");
		field.setAccessible(true);
		try {
			for (int i = 0; i < 100; ++i) {
				var rpc = new TestRpcTimeoutTimerLeak.TimerRpc();
				long sid = service.addRpcContext(rpc);
				rpc.setSessionId(sid);
				var remover = Thread.ofPlatform().start(() -> service.removeRpcContext(sid, rpc));
				rpc.schedule(service, sid, 60_000);
				remover.join(5_000);
				assertNull(field.get(rpc), "iter " + i + ": consumed context must cancel timeout either way");
			}
		} finally {
			service.stop();
		}
	}

	/** 换代注：原断言"consumed context 后 scheduler 队列不增长"对轮子恒真（rpc 定时器不进调度器队列），
	 * 改钉句柄级契约：上下文先消费再 schedule，发布后复查必须立即取消句柄。 */
	@Test
	void contextRemovedBeforeRegistrationCancelsTimeout() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestRpcTimeoutCancellation.BeforeRegistration");
		var field = Rpc.class.getDeclaredField("timeoutTask");
		field.setAccessible(true);
		try {
			var rpc = new TestRpcTimeoutTimerLeak.TimerRpc();
			long sid = service.addRpcContext(rpc);
			rpc.setSessionId(sid);
			assertTrue(service.removeRpcContext(sid, rpc));
			rpc.schedule(service, sid, 60_000);
			assertNull(field.get(rpc), "consumed context must not retain a timeout");
		} finally {
			service.stop();
		}
	}
}
