package Zeze.History;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestApplyMemoryBufferOwnership {
	@Test
	public void storedSlicesSurviveReuseOfCallerBuffers() throws Exception {
		var table = new ApplyDatabaseMemory().open("values");
		var key = new byte[]{9, 1, 9};
		var value = new byte[]{8, 2, 8};
		table.put(key, 1, 1, value, 1, 1);
		key[1] = 3;
		value[1] = 4;
		var stored = table.get(new byte[]{1}, 0, 1);
		assertNotNull(stored);
		assertArrayEquals(new byte[]{2}, stored.toBytes());
		assertNull(table.get(new byte[]{3}, 0, 1));
	}

	@Test
	public void stagedWritesOwnTheirBuffersBeforeCommit() throws Exception {
		var db = new ApplyDatabaseMemory();
		var table = db.open("values");
		try (var txn = db.beginRecordTxn()) {
			var key = new byte[]{1};
			var value = new byte[]{2};
			table.put(key, 0, 1, value, 0, 1);
			key[0] = 3;
			value[0] = 4;
			txn.commit();
		}
		var stored = table.get(new byte[]{1}, 0, 1);
		assertNotNull(stored);
		assertArrayEquals(new byte[]{2}, stored.toBytes());
		assertNull(table.get(new byte[]{3}, 0, 1));
	}

	@Test
	public void stagedRemovalKeepsTheOriginalKey() throws Exception {
		var db = new ApplyDatabaseMemory();
		var table = db.open("values");
		table.put(new byte[]{1}, 0, 1, new byte[]{2}, 0, 1);
		try (var txn = db.beginRecordTxn()) {
			var key = new byte[]{1};
			table.remove(key, 0, 1);
			key[0] = 3;
			txn.commit();
		}
		assertNull(table.get(new byte[]{1}, 0, 1));
	}

	@Test
	public void walkCallbackReceivesIndependentArrays() throws Exception {
		var table = new ApplyDatabaseMemory().open("values");
		table.put(new byte[]{1}, 0, 1, new byte[]{2}, 0, 1);
		table.walk((key, value) -> {
			key[0] = 3;
			value[0] = 4;
			return true;
		});
		var stored = table.get(new byte[]{1}, 0, 1);
		assertNotNull(stored);
		assertArrayEquals(new byte[]{2}, stored.toBytes());
		assertNull(table.get(new byte[]{3}, 0, 1));
	}
}
