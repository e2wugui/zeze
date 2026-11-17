package Zeze.Raft;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAgentStopTimerDrain {
	@Test
	void stopReleasesMutexBeforeDrainingAnAdmittedResendRound() throws Exception {
		Task.tryInitThreadPool();
		var config = RaftConfig.loadFromString("""
				<raft Name=""><node Host="127.0.0.1" Port="19561"/></raft>
				""");
		var agent = new Agent("TestAgentStopTimerDrain", config, new Config());
		var timerField = Agent.class.getDeclaredField("resendTask");
		timerField.setAccessible(true);
		((Future<?>)Objects.requireNonNull(timerField.get(agent))).cancel(false);
		var mutexField = Agent.class.getDeclaredField("mutex");
		mutexField.setAccessible(true);
		var mutex = (ReentrantLock)mutexField.get(agent);
		var entered = new CountDownLatch(1);
		var resume = new CountDownLatch(1);
		var acquired = new AtomicBoolean();
		var timer = TaskSpec.ofAction(() -> {
			entered.countDown();
			boolean interrupted = false;
			while (resume.getCount() != 0) {
				try {
					resume.await();
				} catch (InterruptedException ignored) {
					interrupted = true;
				}
			}
			// Simulate resend's lock acquisition. Bound the wait so the old ABBA implementation
			// fails an assertion without leaving a permanently deadlocked timer thread behind.
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
			while (!acquired.get() && System.nanoTime() < deadline) {
				try {
					if (mutex.tryLock(20, TimeUnit.MILLISECONDS)) {
						try {
							acquired.set(true);
						} finally {
							mutex.unlock();
						}
					}
				} catch (InterruptedException ignored) {
					interrupted = true;
				}
			}
			if (interrupted)
				Thread.currentThread().interrupt();
		}).schedulePeriodNow(0, 60_000);
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			// Release the admitted body exactly when stop begins cancel/join, after its gate is held.
			timerField.set(agent, new Future<Object>() {
				@Override public boolean cancel(boolean interrupt) {
					resume.countDown();
					return timer.cancel(interrupt);
				}
				@Override public boolean isCancelled() { return timer.isCancelled(); }
				@Override public boolean isDone() { return timer.isDone(); }
				@Override public Object get() throws java.util.concurrent.ExecutionException, InterruptedException {
					return timer.get();
				}
				@Override public Object get(long timeout, TimeUnit unit) throws java.util.concurrent.ExecutionException,
						InterruptedException, java.util.concurrent.TimeoutException { return timer.get(timeout, unit); }
			});
			agent.stop();
			assertTrue(acquired.get(), "admitted resend must acquire Agent.mutex before cancel finishes");
			assertTrue(timer.isCancelled());
			assertNull(agent.getClient());
		} finally {
			resume.countDown();
			timer.cancel(true);
			agent.stop();
		}
	}
}
