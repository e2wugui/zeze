package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Config;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftLog;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRecoveryTimerLockOrder {

	private static final class Fixture implements AutoCloseable {
		final Dbh2StateMachine sm = new Dbh2StateMachine(null) {
			@Override
			long undoResurrectGraceMillis() {
				return 0; // Only the tombstone path is exercised; no Dbh2 or coordinator is needed.
			}
		};
		final Raft raft;

		Fixture(Path home) throws Exception {
			Task.tryInitThreadPool();
			var conf = RaftConfig.loadFromString("""
					<raft Name="127.0.0.1:19640" DbHome="%s">
					    <node Host="127.0.0.1" Port="19640"/>
					    <node Host="127.0.0.1" Port="19641"/>
					    <node Host="127.0.0.1" Port="19642"/>
					</raft>
					""".formatted(home));
			// Construct local storage without starting the service or binding any port.
			raft = new Raft(sm, null, conf, new Config());
			cancelTimer(raft, Raft.class, "timerTask");
			cancelTimer(sm, Dbh2StateMachine.class, "timer");
			raft.getLogSequence().trySetTerm(1);
			var batch = new BBatch.Data();
			batch.setTid(1);
			sm.prepareBatch(batch); // Empty batch does not require an Application or key locks.
			sm.undoBatch(1, false);
			assertTrue(sm.isTombstonedPending(1));
		}

		void setLeader(boolean value) throws Exception {
			var field = Raft.class.getDeclaredField("state");
			field.setAccessible(true);
			field.set(raft, value ? Raft.RaftState.Leader : Raft.RaftState.Follower);
		}

		void tick() throws Exception {
			var method = Dbh2StateMachine.class.getDeclaredMethod("onTimer");
			method.setAccessible(true);
			method.invoke(sm);
		}

		@Override
		public void close() throws Exception {
			raft.shutdown();
			sm.close();
		}
	}

	private static void cancelTimer(Object owner, Class<?> type, String name) throws Exception {
		var field = type.getDeclaredField(name);
		field.setAccessible(true);
		var timer = (Future<?>)field.get(owner);
		if (timer != null)
			timer.cancel(true);
		field.set(owner, null);
	}

	private static void checkCancelWhileRecoveryOwnsRaft(Path home, boolean leader) throws Exception {
		try (var fixture = new Fixture(home)) {
			fixture.setLeader(leader);
			var bodyEntered = new CountDownLatch(1);
			var bodyFinished = new CountDownLatch(1);
			var cancelFinished = new CountDownLatch(1);
			var failure = new AtomicReference<@Nullable Throwable>();
			// restore/reset invoke close while holding exactly this mutex. The test owns it so
			// finally can release it even when the old implementation forms the lock cycle.
			fixture.raft.lock();
			var timer = TaskSpec.ofAction(() -> {
				bodyEntered.countDown();
				try {
					fixture.tick();
				} catch (Throwable e) {
					failure.set(e);
				} finally {
					bodyFinished.countDown();
				}
			}).schedulePeriodNow(0, 60_000);
			var cancelThread = Thread.ofVirtual().unstarted(() -> {
				timer.cancel(true); // Actual TimerFuture.cancel: interrupt, then join the body lock.
				cancelFinished.countDown();
			});
			try {
				assertTrue(bodyEntered.await(5, TimeUnit.SECONDS));
				cancelThread.start();
				assertTrue(cancelFinished.await(1, TimeUnit.SECONDS),
						"snapshot recovery must be able to join the timer while owning the Raft mutex");
			} finally {
				fixture.raft.unlock(); // Resolves the old lock cycle; no abandoned timer/thread.
				timer.cancel(true);
				cancelThread.join(10_000);
				assertFalse(cancelThread.isAlive(), "cancellation thread must be drained");
				assertTrue(bodyFinished.await(10, TimeUnit.SECONDS), "timer body must be drained");
			}
			assertNull(failure.get());
			assertTrue(fixture.sm.isTombstonedPending(1), "a skipped round must retain the recovery obligation");
		}
	}

	@Test
	public void expiredTombstoneTimerCancelsWhileRecoveryOwnsRaftLock(@TempDir Path home) throws Exception {
		checkCancelWhileRecoveryOwnsRaft(home, true);
	}

	@Test
	public void followerTimerCancelsWhileRecoveryOwnsRaftLock(@TempDir Path home) throws Exception {
		checkCancelWhileRecoveryOwnsRaft(home, false);
	}

	@Test
	public void leaderTimerRecordsExpiryBeforeTheTombstoneIsRemoved(@TempDir Path home) throws Exception {
		try (var fixture = new Fixture(home)) {
			fixture.setLeader(true);
			var sequence = fixture.raft.getLogSequence();
			var before = sequence.getLastIndex();
			fixture.tick();
			assertEquals(before + 1, sequence.getLastIndex(), "an available leader must record the expiry");
			assertTrue(fixture.sm.isTombstonedPending(1), "admission alone does not finalize the tombstone");
			var read = LogSequence.class.getDeclaredMethod("readLog", long.class);
			read.setAccessible(true);
			fixture.raft.lock();
			try {
				fixture.sm.apply((RaftLog)read.invoke(sequence, before + 1));
			} finally {
				fixture.raft.unlock();
			}
			assertFalse(fixture.sm.isTombstonedPending(1), "only applying the replicated expiry finalizes it");
		}
	}
}
