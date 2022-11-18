package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;

public class DatabaseDynamoDb extends Database {
	private final AmazonDynamoDB dynamoDbClient;

	public DatabaseDynamoDb(Config.DatabaseConf conf) {
		super(conf);

		var dynamoConf = conf.getDynamoConf();
		dynamoDbClient = AmazonDynamoDBClientBuilder.standard()
				.withRegion(dynamoConf.region)
				.enableEndpointDiscovery()
				.build();
	}

	@Override
	public Table openTable(String name) {
		return new TableDynamoDb(name);
	}

	@Override
	public Transaction beginTransaction() {
		return new TransDynamoDb();
	}

	private class TransDynamoDb implements Transaction {
		private final ArrayList<TransactWriteItem> writes = new ArrayList<>();

		public TransDynamoDb() {
		}

		@Override
		public void commit() {
			// 应该是异常报错。
			dynamoDbClient.transactWriteItems(new TransactWriteItemsRequest().withTransactItems(writes));
		}

		@Override
		public void rollback() {
			// 不需要rollback。
		}

		@Override
		public void close() throws Exception {
			writes.clear();
		}

		void replace(String tableName, ByteBuffer key, ByteBuffer value) {
			var put = new Put().withTableName(tableName);
			put.addItemEntry("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.Size())));
			put.addItemEntry("value", new AttributeValue().withB(java.nio.ByteBuffer.wrap(value.Bytes, value.ReadIndex, value.Size())));
			writes.add(new TransactWriteItem().withPut(put));
		}

		void remove(String tableName, ByteBuffer key) {
			var delete = new Delete().withTableName(tableName);
			delete.addKeyEntry("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.Size())));
			writes.add(new TransactWriteItem().withDelete(delete));
		}
	}

	private class TableDynamoDb implements Database.Table {
		private final static KeySchemaElement keySchema = new KeySchemaElement("key", KeyType.HASH);
		private final static AttributeDefinition valueAttribute = new AttributeDefinition("value", ScalarAttributeType.B);

		private final String name;
		private boolean isNew;

		public TableDynamoDb(String name) {
			this.name = name;

			var attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(valueAttribute);
			var keySchemas = new ArrayList<KeySchemaElement>();
			keySchemas.add(keySchema);
			var provisionedThroughput = new ProvisionedThroughput(10L, 10L);
			try {
				dynamoDbClient.createTable(attributeDefinitions, name, keySchemas, provisionedThroughput);
				isNew = true;
			} catch (ResourceInUseException exists) {
				isNew = false;
			}
			// 其他异常抛出去。
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public Database getDatabase() {
			return DatabaseDynamoDb.this;
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			var keyPrimary = new HashMap<String, AttributeValue>();
			keyPrimary.put("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.Size())));
			var req = new GetItemRequest(name, keyPrimary);
			var item = dynamoDbClient.getItem(req).getItem();

			if (null == item)
				return null;

			var value = item.get("value").getB();
			return ByteBuffer.Wrap(value.array(), value.arrayOffset(), value.limit());
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var myt = (TransDynamoDb)t;
			myt.replace(name, key, value);
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			var myt = (TransDynamoDb)t;
			myt.remove(name, key);
		}

		private static byte[] copyIf(java.nio.ByteBuffer bb) {
			if (bb.limit() == bb.capacity() && bb.arrayOffset() == 0)
				return bb.array();
			return Arrays.copyOfRange(bb.array(), bb.arrayOffset(), bb.limit());
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			var attributesToGet = new ArrayList<String>();
			attributesToGet.add("key");
			attributesToGet.add("value");
			var req = new ScanRequest();
			req.setAttributesToGet(attributesToGet);
			long count = 0;
			while (true) {
				var scanResult = dynamoDbClient.scan(req);
				for (var item : scanResult.getItems()) {
					var key = copyIf(item.get("key").getB());
					var value = copyIf(item.get("value").getB());
					if (!callback.handle(key, value))
						return count;
					count ++;
				}
				if (scanResult.getLastEvaluatedKey() == null)
					break;

				req = new ScanRequest();
				req.setAttributesToGet(attributesToGet);
				req.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
			}
			return count;
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			var attributesToGet = new ArrayList<String>();
			attributesToGet.add("key");
			var req = new ScanRequest();
			req.setAttributesToGet(attributesToGet);
			long count = 0;
			while (true) {
				var scanResult = dynamoDbClient.scan(req);
				for (var item : scanResult.getItems()) {
					var key = copyIf(item.get("key").getB());
					if (!callback.handle(key))
						return count;
					count ++;
				}
				if (scanResult.getLastEvaluatedKey() == null)
					break;

				req = new ScanRequest();
				req.setAttributesToGet(attributesToGet);
				req.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
			}
			return count;
		}

		@Override
		public void close() {

		}
	}
}
