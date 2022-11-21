package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
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
		// 这里验证证书是通过配置文件指定的。
		// todo 增加参数指定endpoint，用来支持明确的服务器，便于测试。
		dynamoDbClient = AmazonDynamoDBClientBuilder.standard()
				.withRegion(dynamoConf.region)
				.enableEndpointDiscovery()
				.build();
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesDynamoDb());
	}

	@Override
	public Table openTable(String name) {
		return new TableDynamoDb(name);
	}

	@Override
	public Transaction beginTransaction() {
		return new TransDynamoDb();
	}

	private class OperatesDynamoDb implements Database.Operates {
		private final TableDynamoDb dataWithVersion;

		public OperatesDynamoDb() {
			dataWithVersion = (TableDynamoDb)openTable("zeze.OperatesDynamoDb.Schemas");
		}

		@Override
		public void setInUse(int localId, String global) {
			// 暂时不支持
		}

		@Override
		public int clearInUse(int localId, String global) {
			// 暂时不支持
			return 0;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			var dv = getDataWithVersion(key);
			if (dv.version != version)
				return KV.create(version, false);

			dv.version = ++version;
			dv.data = data;
			var value = ByteBuffer.Allocate();
			dv.encode(value);

			var trans = beginTransaction();
			dataWithVersion.replace(trans, key, value);
			trans.commit();
			return KV.create(version, true);
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			var result = new DataWithVersion();
			var bb = dataWithVersion.find(key);
			if (null != bb)
				result.decode(bb);
			return result;
		}
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

	private static final KeySchemaElement keySchema = new KeySchemaElement("key", KeyType.HASH);
	private static final AttributeDefinition valueAttribute = new AttributeDefinition("value", ScalarAttributeType.B);

	private class TableDynamoDb implements Database.Table {
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
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			// todo dynamodb walk page
			return null;
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