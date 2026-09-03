package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;

public class DatabaseDynamoDb extends Database {
	private final AmazonDynamoDB dynamoDbClient;

	public DatabaseDynamoDb(Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);

		var dynamoConf = conf.getDynamoConf();
		// 这里验证证书是通过配置文件指定的。
		// 增加参数指定endpoint，用来支持明确的服务器，便于测试。
		dynamoDbClient = AmazonDynamoDBClientBuilder.standard()
				.withRegion(dynamoConf.region)
				.enableEndpointDiscovery()
				.build();
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesDynamoDb());
	}

	@Override
	public Table openTable(String name, int id) {
		return new TableDynamoDb(name);
	}

	@Override
	public Transaction beginTransaction() {
		return new TransDynamoDb();
	}

	private class OperatesDynamoDb implements Database.Operates {
		private final TableDynamoDb dataWithVersion;

		public OperatesDynamoDb() {
			var schemaTableName = "Zeze_OperatesDynamoDb_Schemas";
			dataWithVersion = (TableDynamoDb)openTable(schemaTableName, Bean.hash32(schemaTableName));
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
			// 读-判-写必须原子：多实例并发启动（tryLock 恒真的后端）同时进入 schemasCompatible 时，
			// 读-判-写会丢失更新，级联出重复 renameTable 丢表数据。
			// 这里用“完整旧值”做服务端条件写（PutItem + ConditionExpression）：
			// value 是 DataWithVersion 的确定性编码（decode->encode 字节往返一致），
			// 完整值相等蕴含 version 相等。并发修改/插入时条件失败抛
			// ConditionalCheckFailedException，返回 false 让调用方(schemasCompatible)重读重试。
			var oldBb = dataWithVersion.find(key);
			var oldBytes = oldBb != null ? oldBb.CopyIf() : null;
			if (oldBytes != null && DataWithVersion.decode(oldBytes).version != version)
				return KV.create(version, false);

			var dv = new DataWithVersion();
			var newVersion = version + 1;
			dv.version = newVersion;
			dv.data = data;
			var value = ByteBuffer.Allocate(5 + 9 + dv.data.size());
			dv.encode(value);

			var item = new HashMap<String, AttributeValue>();
			item.put("key", new AttributeValue().withB(
					java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.size())));
			item.put("value", new AttributeValue().withB(
					java.nio.ByteBuffer.wrap(value.Bytes, value.ReadIndex, value.size())));
			var req = new PutItemRequest()
					.withTableName(dataWithVersion.name)
					.withItem(item)
					.withConditionExpression(oldBytes != null ? "#v = :oldv" : "attribute_not_exists(#k)")
					.withExpressionAttributeNames(oldBytes != null ? Map.of("#v", "value") : Map.of("#k", "key"))
					.withExpressionAttributeValues(oldBytes != null
							? Map.of(":oldv", new AttributeValue().withB(java.nio.ByteBuffer.wrap(oldBytes)))
							: null);
			try {
				dynamoDbClient.putItem(req);
			} catch (ConditionalCheckFailedException e) {
				return KV.create(version, false); // 并发修改/插入：调用方(schemasCompatible)重读重试。
			}
			return KV.create(newVersion, true);
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			var result = new DataWithVersion();
			var bb = dataWithVersion.find(key);
			if (bb != null)
				result.decode(bb);
			return result;
		}
	}

	private final class TransDynamoDb implements Transaction {
		// DynamoDb TransactWriteItems 单请求最多 100 个 item（服务端硬限）。
		private static final int TRANSACT_WRITE_ITEMS_MAX = 100;
		private final ArrayList<TransactWriteItem> writes = new ArrayList<>();

		@Override
		public void commit() {
			// FlushSet 默认攒批阈值（50 rrs / 10000 条记录）远超单事务 100 item 上限，
			// 一次性提交会让 checkpoint flush 永久失败（脏记录只增不减，直至 OOM）。
			// 这里按上限分批顺序提交：每批内部保持 transactWriteItems 原子；
			// 跨批失败时由 Checkpoint 保留 rrs 脏标记、整批幂等重试自愈
			// （replace/remove 均为最终值覆盖写，重放安全；与 DatabaseTikv raw 模式
			// batchPut/batchDelete 的非原子批处理同型）。失败时 transactWriteItems 抛异常上抛。
			for (var begin = 0; begin < writes.size(); begin += TRANSACT_WRITE_ITEMS_MAX) {
				var items = writes.subList(begin, Math.min(begin + TRANSACT_WRITE_ITEMS_MAX, writes.size()));
				dynamoDbClient.transactWriteItems(new TransactWriteItemsRequest().withTransactItems(items));
			}
		}

		@Override
		public void rollback() {
			// 不需要rollback。
		}

		@Override
		public void close() {
			writes.clear();
		}

		void replace(String tableName, ByteBuffer key, ByteBuffer value) {
			var put = new Put().withTableName(tableName);
			put.addItemEntry("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.size())));
			put.addItemEntry("value", new AttributeValue().withB(java.nio.ByteBuffer.wrap(value.Bytes, value.ReadIndex, value.size())));
			writes.add(new TransactWriteItem().withPut(put));
		}

		void remove(String tableName, ByteBuffer key) {
			var delete = new Delete().withTableName(tableName);
			delete.addKeyEntry("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.size())));
			writes.add(new TransactWriteItem().withDelete(delete));
		}
	}

	private static final KeySchemaElement keySchema = new KeySchemaElement("key", KeyType.HASH);
	private static final AttributeDefinition valueAttribute = new AttributeDefinition("value", ScalarAttributeType.B);

	private class TableDynamoDb extends Database.AbstractKVTable {
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
			keyPrimary.put("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.size())));
			var req = new GetItemRequest(name, keyPrimary);
			var item = dynamoDbClient.getItem(req).getItem();
			if (item == null)
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
		public long walk(TableWalkHandleRaw callback) throws Exception {
			var attributesToGet = new ArrayList<String>();
			attributesToGet.add("key");
			attributesToGet.add("value");
			var req = new ScanRequest();
			req.setTableName(name);
			req.setAttributesToGet(attributesToGet);
			long count = 0;
			while (true) {
				var scanResult = dynamoDbClient.scan(req);
				for (var item : scanResult.getItems()) {
					var key = copyIf(item.get("key").getB());
					var value = copyIf(item.get("value").getB());
					if (!callback.handle(key, value))
						return count;
					count++;
				}
				if (scanResult.getLastEvaluatedKey() == null)
					break;

				req = new ScanRequest();
				req.setTableName(name);
				req.setAttributesToGet(attributesToGet);
				req.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
			}
			return count;
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) throws Exception {
			var attributesToGet = new ArrayList<String>();
			attributesToGet.add("key");
			var req = new ScanRequest();
			req.setTableName(name);
			req.setAttributesToGet(attributesToGet);
			long count = 0;
			while (true) {
				var scanResult = dynamoDbClient.scan(req);
				for (var item : scanResult.getItems()) {
					var key = copyIf(item.get("key").getB());
					if (!callback.handle(key))
						return count;
					count++;
				}
				if (scanResult.getLastEvaluatedKey() == null)
					break;

				req = new ScanRequest();
				req.setTableName(name);
				req.setAttributesToGet(attributesToGet);
				req.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
			}
			return count;
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;

			var req = new ScanRequest();
			req.setTableName(name);
			req.setAttributesToGet(List.of("key", "value"));
			if (exclusiveStartKey != null) {
				req.setExclusiveStartKey(Map.of("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(
						exclusiveStartKey.Bytes, exclusiveStartKey.ReadIndex, exclusiveStartKey.size()))));
			}
			var scanResult = dynamoDbClient.scan(req);
			byte[] lastKey = null;
			for (var item : scanResult.getItems()) {
				lastKey = copyIf(item.get("key").getB());
				if (!callback.handle(lastKey, copyIf(item.get("value").getB())) || --proposeLimit == 0)
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;

			var req = new ScanRequest();
			req.setTableName(name);
			req.setAttributesToGet(List.of("key"));
			if (exclusiveStartKey != null) {
				req.setExclusiveStartKey(Map.of("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(
						exclusiveStartKey.Bytes, exclusiveStartKey.ReadIndex, exclusiveStartKey.size()))));
			}
			var scanResult = dynamoDbClient.scan(req);
			byte[] lastKey = null;
			for (var item : scanResult.getItems()) {
				lastKey = copyIf(item.get("key").getB());
				if (!callback.handle(lastKey) || --proposeLimit == 0)
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public void close() {
		}
	}
}
