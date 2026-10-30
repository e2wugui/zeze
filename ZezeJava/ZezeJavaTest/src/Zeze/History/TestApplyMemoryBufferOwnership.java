package Zeze.History;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestApplyMemoryBufferOwnership {
	@Test
	public void retainsInputAndWalksSlices() throws Exception {
		var db = new ApplyDatabaseMemory();
		var table = db.open("values");
		var key = new byte[]{9, 1, 9};
		var value = new byte[]{8, 2, 8};
		try (var txn = db.beginRecordTxn()) {
			table.put(key, 1, 1, value, 1, 1);
			txn.commit();
		}
		var stored = table.get(new byte[]{1}, 0, 1);
		assertNotNull(stored);
		assertSame(value, stored.bytesUnsafe());
		table.walk((walkKey, walkValue) -> {
			assertArrayEquals(new byte[]{1}, walkKey);
			assertArrayEquals(new byte[]{2}, walkValue);
			return true;
		});
		try (var txn = db.beginRecordTxn()) {
			table.remove(key, 1, 1);
			txn.commit();
		}
		assertTrue(table.isEmpty());
	}
}
