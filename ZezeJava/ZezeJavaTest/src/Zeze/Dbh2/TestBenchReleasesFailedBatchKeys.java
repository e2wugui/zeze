package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Transaction.Database.AbstractKVTable;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Fast
public class TestBenchReleasesFailedBatchKeys {
	@Test
	void failedWriteReleasesEveryReservedKey() throws Exception {
		verifyFailure(false);
	}

	@Test
	void failedCommitReleasesEveryReservedKey() throws Exception {
		verifyFailure(true);
	}

	private static void verifyFailure(boolean failCommit) throws Exception {
		Task.tryInitThreadPool();
		var running = new OutObject<>(true);
		var database = mock(Database.class);
		var transaction = mock(Zeze.Transaction.Database.Transaction.class);
		var table = mock(AbstractKVTable.class);
		when(database.beginTransaction()).thenReturn(transaction);
		if (failCommit) {
			doAnswer(call -> {
				running.value = false;
				throw new IllegalStateException("commit failed");
			}).when(transaction).commit();
		} else {
			doAnswer(call -> {
				running.value = false;
				throw new IllegalStateException("write failed");
			}).when(table).replace(any(), any(), any());
		}
		var client = new BenchClient();
		var successes = new AtomicLong();
		client.startPutTask(running, database, 2, new ArrayList<>(java.util.List.of(table)), 1, successes)
				.get(10, TimeUnit.SECONDS);
		var field = BenchClient.class.getDeclaredField("rrs");
		field.setAccessible(true);
		assertTrue(((Map<?, ?>)field.get(client)).isEmpty());
		assertEquals(0, successes.get());
		verify(transaction).close();
	}
}
