package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestDatabaseBatchLifecycle {
	@Test
	void memoryCompletionConsumesThePendingBatch(@TempDir Path directory) throws Exception {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl(directory.toString());
		var db = new DatabaseMemory(null, conf);
		var table = (Database.AbstractKVTable)db.openTable("records", 0);
		try {
			assertCompletionConsumesWrites(db, table);
		} finally {
			table.clear();
			db.close();
		}
	}

	@Test
	void rocksCompletionConsumesThePendingBatch(@TempDir Path directory) throws Exception {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl(directory.resolve("db").toString());
		var db = new DatabaseRocksDb(null, conf);
		try {
			assertCompletionConsumesWrites(db, (Database.AbstractKVTable)db.openTable("records", 0));
		} finally {
			db.close();
		}
	}

	private static void assertCompletionConsumesWrites(Database db, Database.AbstractKVTable table) throws Exception {
		var discarded = ByteBuffer.Wrap(new byte[]{1});
		var committed = ByteBuffer.Wrap(new byte[]{2});
		try (var txn = db.beginTransaction()) {
			table.replace(txn, discarded, ByteBuffer.Wrap(new byte[]{3}));
			txn.rollback();
			table.replace(txn, committed, ByteBuffer.Wrap(new byte[]{4}));
			txn.commit();
			assertArrayEquals(new byte[]{4}, java.util.Objects.requireNonNull(table.find(committed)).Copy());

			try (var newer = db.beginTransaction()) {
				table.replace(newer, committed, ByteBuffer.Wrap(new byte[]{5}));
				newer.commit();
			}
			txn.commit();
		}
		assertNull(table.find(discarded));
		assertArrayEquals(new byte[]{5}, java.util.Objects.requireNonNull(table.find(committed)).Copy());
	}

	@Test
	void dynamoSendsOnlyTheCurrentBatch() throws Exception {
		var calls = new AtomicInteger();
		var client = new AbstractAmazonDynamoDB() {
			@Override
			public CreateTableResult createTable(List<AttributeDefinition> attributes, String tableName,
					List<KeySchemaElement> keys, ProvisionedThroughput throughput) {
				return new CreateTableResult();
			}

			@Override
			public TransactWriteItemsResult transactWriteItems(TransactWriteItemsRequest request) {
				calls.incrementAndGet();
				assertEquals(1, request.getTransactItems().size());
				return new TransactWriteItemsResult();
			}

			@Override
			public void shutdown() {
			}
		};
		var conf = new Config.DatabaseConf();
		conf.setDisableOperates(true);
		var db = new DatabaseDynamoDb(null, conf, client);
		try (var txn = db.beginTransaction()) {
			var table = (Database.AbstractKVTable)db.openTable("records", 0);
			table.replace(txn, ByteBuffer.Wrap(new byte[]{1}), ByteBuffer.Wrap(new byte[]{2}));
			txn.rollback();
			txn.commit();
			assertEquals(0, calls.get());

			table.replace(txn, ByteBuffer.Wrap(new byte[]{1}), ByteBuffer.Wrap(new byte[]{3}));
			txn.commit();
			txn.commit();
			assertEquals(1, calls.get());
		} finally {
			db.close();
		}
	}
}
