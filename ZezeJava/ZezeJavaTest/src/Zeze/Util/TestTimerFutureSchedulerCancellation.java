package Zeze.Util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestTimerFutureSchedulerCancellation {
	@Test
	public void schedulerShutdownIsVisibleAsCancellation() {
		try (var scheduler = new ScheduledThreadPoolExecutor(1)) {
			var future = new TimerFuture<Long>("periodic", 1000);
			future.setFuture(scheduler.scheduleWithFixedDelay(() -> { }, 1, 1, TimeUnit.DAYS));
			scheduler.shutdown();
			assertTrue(future.isDone());
			assertThrows(CancellationException.class, future::get);
			assertTrue(future.isCancelled(), "底层调度被取消后，包装Future也必须报告取消");
		}
	}
}
