package Zeze.MQ;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestMQSingleCloseFillBudget {

	private static final class PausedFillFile extends MQFileWithIndex {
		private final CountDownLatch admitting = new CountDownLatch(1);
		private final CountDownLatch resume = new CountDownLatch(1);
		private final CountDownLatch closed = new CountDownLatch(1);

		private PausedFillFile(String home, RocksDatabase database) throws Exception {
			super(home, database, "topic", 0);
		}

		@Override
		public long fillMessage(Queue<BMessage.Data> queue, long head, long end, FillBudget budget) {
			return super.fillMessage(queue, head, end, (bytes, empty) -> {
				admitting.countDown();
				try {
					if (!resume.await(10, TimeUnit.SECONDS))
						throw new AssertionError("fill was not resumed");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
				return budget.admit(bytes, empty);
			});
		}

		@Override
		public void close() throws java.io.IOException {
			super.close();
			closed.countDown();
		}
	}

	private static void setField(MQSingle single, String name, Object value) throws Exception {
		var field = MQSingle.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(single, value);
	}

	@Test
	public void closeRejectsLateFillAccounting(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		var file = new PausedFillFile(manager.getHome(), manager.getRocksDatabase());
		var single = new MQSingle(new MQPartition(manager), "topic", 0, file);
		CompletableFuture<Void> fill = null;
		CompletableFuture<Void> close = null;
		try {
			var message = new BMessage.Data();
			message.setBody(new Binary(new byte[1024]));
			file.appendMessage(message);
			setField(single, "highLoad", 1L);
			fill = CompletableFuture.runAsync(single::pullMessage);
			Assertions.assertTrue(file.admitting.await(5, TimeUnit.SECONDS), "fill reaches the real budget callback");
			setField(single, "messageFillFuture", fill);

			// Exhausting the outer drain envelope is a supported close path: close sets
			// its gate and releases bytes, then waits for the outstanding fill generation.
			close = CompletableFuture.runAsync(() -> {
				try {
					single.close(System.currentTimeMillis() - 1);
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			});
			Assertions.assertTrue(file.closed.await(5, TimeUnit.SECONDS), "close has released the partition budget");
			file.resume.countDown();
			fill.get(5, TimeUnit.SECONDS);
			close.get(5, TimeUnit.SECONDS);

			Assertions.assertEquals(0, single.queueBytes(), "a closed partition must not acquire fill bytes");
			Assertions.assertEquals(0, manager.totalInFlightBytes.get(), "late fill must not leak the manager's budget");
		} finally {
			file.resume.countDown();
			if (fill != null)
				fill.get(10, TimeUnit.SECONDS);
			if (close != null)
				close.get(10, TimeUnit.SECONDS);
			single.close();
			manager.stop();
		}
	}
}
