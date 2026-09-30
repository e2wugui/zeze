package Zeze.MQ;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestMQSingleCloseDrainDeadline {

	private static final class ObservedWait extends CompletableFuture<Void> {
		private final AtomicInteger waits = new AtomicInteger();

		@Override
		public Void get(long timeout, TimeUnit unit) throws TimeoutException {
			waits.incrementAndGet();
			throw new TimeoutException("observed timed wait without blocking the test");
		}
	}

	@Test
	public void expiredEnvelopeSkipsEveryFillWait(@TempDir Path tempDir) throws Exception {
		try (var database = new RocksDatabase(tempDir.toString())) {
			var file = new MQFileWithIndex(tempDir.toString(), database, "topic", 0);
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);
			var future = new ObservedWait();
			var field = MQSingle.class.getDeclaredField("messageFillFuture");
			field.setAccessible(true);
			field.set(single, future);
			try {
				single.close(System.currentTimeMillis() - 1);
				Assertions.assertEquals(0, future.waits.get(),
						"an exhausted aggregate deadline must also skip the escaped-generation wait");
			} finally {
				field.set(single, null);
				single.close();
			}
		}
	}
}
