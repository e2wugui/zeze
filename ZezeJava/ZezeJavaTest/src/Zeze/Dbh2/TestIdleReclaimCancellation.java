package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Config;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestIdleReclaimCancellation {

	private static void checkCancellation(Path home, boolean ownManagerLock) throws Exception {
		Task.tryInitThreadPool();
		// Remote mode creates neither a local CommitRocks database nor a network listener.
		var manager = new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(),
				Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(home).toString()));
		var reclaim = Dbh2AgentManager.class.getDeclaredMethod("reclaimIdleAgents");
		reclaim.setAccessible(true);
		var entered = new CountDownLatch(1);
		var finished = new CountDownLatch(1);
		var canceled = new CountDownLatch(1);
		var failure = new AtomicReference<@Nullable Throwable>();
		// stop owns this same lock while canceling idleReclaimTask. The test owns it so
		// an old implementation's lock cycle can always be released in finally.
		if (ownManagerLock)
			manager.lock();
		var timer = TaskSpec.ofAction(() -> {
			entered.countDown();
			try {
				reclaim.invoke(manager);
			} catch (Throwable e) {
				failure.set(e);
			} finally {
				finished.countDown();
			}
		}).schedulePeriodNow(0, 60_000);
		var cancelThread = Thread.ofVirtual().unstarted(() -> {
			timer.cancel(false); // Same join contract and non-interrupting mode as stop.
			canceled.countDown();
		});
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			cancelThread.start();
			assertTrue(canceled.await(1, TimeUnit.SECONDS),
					"manager stop must be able to join idle reclaim while owning its mutex");
		} finally {
			if (ownManagerLock)
				manager.unlock();
			timer.cancel(false);
			cancelThread.join(10_000);
			assertFalse(cancelThread.isAlive(), "cancellation thread must be drained");
			assertTrue(finished.await(10, TimeUnit.SECONDS), "reclaim body must be drained");
			manager.stop();
		}
		assertNull(failure.get());
	}

	@Test
	public void cancellationCompletesWhileStopOwnsManagerMutex(@TempDir Path home) throws Exception {
		checkCancellation(home, true);
	}

	@Test
	public void cancellationCompletesWhenManagerMutexIsAvailable(@TempDir Path home) throws Exception {
		checkCancellation(home, false);
	}
}
