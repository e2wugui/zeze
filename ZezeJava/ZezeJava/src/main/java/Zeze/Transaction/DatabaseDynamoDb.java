package Zeze.Transaction;

import java.util.Objects;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * AWS DynamoDB 数据库后端：KV 表以 TransactWriteItems 事务落库，
 * Operates 用条件写实现同版本保存。
 */
public class DatabaseDynamoDb extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseDynamoDb.class);

	private final AmazonDynamoDB dynamoDbClient;

	public DatabaseDynamoDb(@Nullable Application zeze, Config.DatabaseConf conf) {
		this(zeze, conf, createClient(conf));
	}

	DatabaseDynamoDb(@Nullable Application zeze, Config.DatabaseConf conf, AmazonDynamoDB client) {
		super(zeze, conf);
		dynamoDbClient = client;
		try {
			setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesDynamoDb());
		} catch (Throwable failure) {
			try {
				client.shutdown();
			} catch (Throwable shutdownFailure) {
				if (failure != shutdownFailure)
					failure.addSuppressed(shutdownFailure);
			}
			throw failure;
		}
	}

	private static AmazonDynamoDB createClient(Config.DatabaseConf conf) {
		var dynamoConf = Objects.requireNonNull(conf.getDynamoConf());
		return AmazonDynamoDBClientBuilder.standard()
				.withRegion(dynamoConf.region)
				.enableEndpointDiscovery()
				.build();
	}

	@Override
	public void close() {
		// 分别捕获，确保 super.close() 异常时仍能 shutdown 客户端连接池。
		try {
			super.close();
		} catch (Throwable e) { // logger.error
			logger.error("Database.close exception:", e);
		}
		try {
			dynamoDbClient.shutdown();
		} catch (Throwable e) { // logger.error
			logger.error("dynamoDbClient.shutdown exception:", e);
		}
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
			// schemas 兼容检查早于用户表 waitReady 读取该表，须在此就绪等待。
			dataWithVersion.waitReady();
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
			// 服务端条件写（完整旧值相等或键不存在），并发冲突由上层 schemasCompatible 重读重试。
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
		private static final int TRANSACT_WRITE_ITEMS_MAX = 100;
		private final ArrayList<TransactWriteItem> writes = new ArrayList<>();

		@Override
		public void commit() {
			// TransactWriteItems 单请求上限 100 项，分批提交仅保证单批原子，需上层控制关联记录集规模。
			for (var begin = 0; begin < writes.size(); begin += TRANSACT_WRITE_ITEMS_MAX) {
				var items = writes.subList(begin, Math.min(begin + TRANSACT_WRITE_ITEMS_MAX, writes.size()));
				dynamoDbClient.transactWriteItems(new TransactWriteItemsRequest().withTransactItems(items));
			}
			writes.clear();
		}

		@Override
		public void rollback() {
			writes.clear();
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
	// AttributeDefinitions 必须与 KeySchema 主键属性名一致。
	private static final AttributeDefinition keyAttribute = new AttributeDefinition("key", ScalarAttributeType.B);

	private class TableDynamoDb extends Database.AbstractKVTable {
		private final String name;
		private boolean isNew;

		public TableDynamoDb(String name) {
			this.name = name;

			var attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(keyAttribute);
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

		/** createTable 为异步操作，轮询 describeTable 直至表进入 ACTIVE/UPDATING。 */
		@Override
		public void waitReady() {
			var deadline = System.currentTimeMillis() + 60_000;
			while (true) {
				var status = dynamoDbClient.describeTable(name).getTable().getTableStatus();
				if (TableStatus.ACTIVE.toString().equals(status) || TableStatus.UPDATING.toString().equals(status))
					return;
				if (System.currentTimeMillis() > deadline)
					throw new IllegalStateException("DynamoDb table not ready: " + name + ", status=" + status);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("interrupted while waiting table ready: " + name, e);
				}
			}
		}

		@Override
		public Database getDatabase() {
			return DatabaseDynamoDb.this;
		}

		@Override
		public @Nullable ByteBuffer find(ByteBuffer key) {
			var keyPrimary = new HashMap<String, AttributeValue>();
			keyPrimary.put("key", new AttributeValue().withB(java.nio.ByteBuffer.wrap(key.Bytes, key.ReadIndex, key.size())));
			var req = new GetItemRequest(name, keyPrimary);
			// 必须使用强一致读，避免记录跨进程接管后冷读到过期副本。
			req.setConsistentRead(true);
			var item = dynamoDbClient.getItem(req).getItem();
			if (item == null)
				return null;

			var value = item.get("value").getB();
			return ByteBuffer.Wrap(copyIf(value));
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			checkKvKeyLength(name, key);
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
			// Scan 与 find 同因：walk 供 checkpoint 与工具路径读取，同样要求强一致读。
			req.setConsistentRead(true);
			long count = 0;
			while (true) {
				var scanResult = dynamoDbClient.scan(req);
				for (var item : scanResult.getItems()) {
					var key = copyIf(item.get("key").getB());
					var value = copyIf(item.get("value").getB());
					count++; // 被回调且返回 false 的中断项也计入（见 AbstractKVTable.walk 契约）
					if (!callback.handle(key, value))
						return count;
				}
				if (scanResult.getLastEvaluatedKey() == null)
					break;

				req = new ScanRequest();
				req.setTableName(name);
				req.setAttributesToGet(attributesToGet);
				req.setConsistentRead(true);
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
			req.setConsistentRead(true);
			long count = 0;
			while (true) {
				var scanResult = dynamoDbClient.scan(req);
				for (var item : scanResult.getItems()) {
					var key = copyIf(item.get("key").getB());
					count++; // 中断项计入返回值，与其他后端对齐
					if (!callback.handle(key))
						return count;
				}
				if (scanResult.getLastEvaluatedKey() == null)
					break;

				req = new ScanRequest();
				req.setTableName(name);
				req.setAttributesToGet(attributesToGet);
				req.setConsistentRead(true);
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
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			// 表仅有 hash key，Scan 无 key 全序且游标不支持跨轮持久化，不支持分页遍历。
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
		}
	}
}
