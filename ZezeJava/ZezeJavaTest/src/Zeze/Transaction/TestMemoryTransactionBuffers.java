package Zeze.Transaction;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestMemoryTransactionBuffers {
	@Test
	void pendingAndCommittedWritesOwnTheirBytes() throws Exception {
		var config = new Config.DatabaseConf();
		config.setDatabaseUrl("memory_transaction_buffers");
		var database = new DatabaseMemory(null, config);
		var table = (Database.AbstractKVTable)database.openTable("records", 0);
		try {
			var key = new byte[]{1};
			var value = new byte[]{2};
			try (var transaction = database.beginTransaction()) {
				table.replace(transaction, ByteBuffer.Wrap(key), ByteBuffer.Wrap(value));
				value[0] = 3;
				transaction.commit();
			}
			key[0] = 4;
			var originalKey = ByteBuffer.Wrap(new byte[]{1});
			var stored = table.find(originalKey);
			assertNotNull(stored);
			assertArrayEquals(new byte[]{2}, stored.Copy());
			assertNull(table.find(ByteBuffer.Wrap(key)));

			key[0] = 1;
			try (var transaction = database.beginTransaction()) {
				table.remove(transaction, ByteBuffer.Wrap(key));
				key[0] = 4;
				transaction.commit();
			}
			assertNull(table.find(originalKey));
		} finally {
			table.clear();
			database.close();
		}
	}
}
