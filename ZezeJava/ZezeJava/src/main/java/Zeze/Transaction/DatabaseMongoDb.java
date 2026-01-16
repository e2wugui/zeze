package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import com.mongodb.MongoCommandException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.Binary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseMongoDb extends Database {
	final @NotNull MongoClient mongoClient;
	final @NotNull MongoDatabase mongoDatabase;

	public DatabaseMongoDb(Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);
		mongoClient = MongoClients.create(conf.getDatabaseUrl());
		mongoDatabase = mongoClient.getDatabase(conf.getDatabaseName());
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesMongoDb());
	}

	@Override
	public @NotNull Table openTable(@NotNull String name, int id) {
		return new TableMongoDb(name);
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new MongoTrans(mongoClient.startSession());
	}

	@Override
	public void close() {
		super.close();
		mongoClient.close();
	}

	private class OperatesMongoDb implements Operates {
		private final TableMongoDb dataWithVersion;

		public OperatesMongoDb() {
			var schemaTableName = "zeze.OperatesMongoDb.Schemas";
			dataWithVersion = (TableMongoDb)openTable(schemaTableName, Bean.hash32(schemaTableName));
		}

		@Override
		public void setInUse(int localId, @NotNull String global) {

		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			return 0;
		}

		@Override
		public @Nullable KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			var dv = getDataWithVersion(key);
			if (dv.version != version)
				return KV.create(version, false);

			dv.version = ++version;
			dv.data = data;
			var value = ByteBuffer.Allocate(5 + 9 + dv.data.size());
			dv.encode(value);

			try (var trans = beginTransaction()) {
				dataWithVersion.replace(trans, key, value);
				trans.commit();
			} catch (Exception e) {
				Task.forceThrow(e);
			}
			return KV.create(version, true);
		}

		@Override
		public @Nullable DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			var result = new DataWithVersion();
			var bb = dataWithVersion.find(key);
			if (bb != null)
				result.decode(bb);
			return result;
		}
	}

	public static class MongoTrans implements Transaction {
		private final @NotNull ClientSession session;

		public MongoTrans(@NotNull ClientSession session) {
			this.session = session;
			session.startTransaction();
		}

		public @NotNull ClientSession getSession() {
			return session;
		}

		@Override
		public void commit() {
			session.commitTransaction();
		}

		@Override
		public void rollback() {
			session.abortTransaction();
		}

		@Override
		public void close() throws Exception {
			session.close();
		}
	}

	public final class TableMongoDb extends AbstractKVTable {
		private final @NotNull String name;
		private final boolean isNew;
		private final MongoCollection<Document> collection;
		private boolean dropped;

		public TableMongoDb(@NotNull String name) {
			this.name = name;
			var isNew = true;
			try {
				mongoDatabase.createCollection(name);
			} catch (MongoCommandException e) {
				if (e.getErrorCode() == 48)
					isNew = false; // collection 已经存在。
				Task.forceThrow(e);
			}
			this.isNew = isNew;
			this.collection = mongoDatabase.getCollection(name);
		}

		@Override
		public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
			if (dropped)
				return null;

			var filter = Filters.eq("_id", key.CopyIf());
			var doc  = collection.find(filter).first();
			if (doc == null)
				return null;
			var valueBinary = doc.get("value", Binary.class);
			var result = valueBinary != null ? valueBinary.getData() : null;
			if (result == null)
				return null;
			return ByteBuffer.Wrap(result);
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			if (dropped)
				return;

			var txn = (MongoTrans)t;
			var keyBinary = new Binary(key.CopyIf());
			var doc = new Document("_id", keyBinary).append("value", new Binary(value.CopyIf()));
			var result = collection.replaceOne(txn.getSession(), new Document("_id", keyBinary), doc,
					new ReplaceOptions().upsert(true).bypassDocumentValidation(false));
			var success = result.getMatchedCount() > 0 || result.getUpsertedId() != null;
			if (!success)
				throw new RuntimeException("replaceOne error");
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			if (dropped)
				return;

			var txn = (MongoTrans)t;
			var filter = new Document("_id", new Binary(key.CopyIf()));
			var options = new DeleteOptions();
			collection.deleteOne(txn.getSession(), filter, options); // skip not exist.
		}


		public static byte[] getByteArray(Document doc, String fieldName) {
			Object value = doc.get(fieldName);
			if (value == null)
				throw new NullPointerException(fieldName + " is null");
			if (value instanceof byte[])
				return (byte[])value;
			if (value instanceof Binary)
				return ((Binary)value).getData();
			throw new ClassCastException("field '" + fieldName + "' type " + value.getClass() + " cast to byte[]");
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped)
				return 0;
			var iterable = collection.find();
			iterable.sort(Sorts.ascending("_id"));
			var countWalked = new OutLong();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					var value = getByteArray(document,"value");
					countWalked.value++;
					callback.handle(key, value);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return countWalked.value;
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped)
				return 0;
			var iterable = collection.find();
			iterable.projection(Projections.include("_id"));
			iterable.sort(Sorts.ascending("_id"));
			var countWalked = new OutLong();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					countWalked.value++;
					callback.handle(key);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return countWalked.value;
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped)
				return 0;
			var iterable = collection.find();
			iterable.sort(Sorts.descending("_id"));
			var countWalked = new OutLong();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					var value = getByteArray(document,"value");
					countWalked.value++;
					callback.handle(key, value);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return countWalked.value;
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped)
				return 0;
			var iterable = collection.find();
			iterable.projection(Projections.include("_id"));
			iterable.sort(Sorts.descending("_id"));
			var countWalked = new OutLong();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					countWalked.value++;
					callback.handle(key);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return countWalked.value;
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped)
				return null;

			var start = exclusiveStartKey != null ? exclusiveStartKey.CopyIf() : null;
			var iterable = start == null ? collection.find() : collection.find(Filters.gt("_id", start));
			iterable.limit(proposeLimit);
			iterable.sort(Sorts.ascending("_id"));
			var lastKey = new OutObject<byte[]>();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					var value = getByteArray(document,"value");
					lastKey.value = key;
					callback.handle(key, value);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return lastKey.value != null ? ByteBuffer.Wrap(lastKey.value) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped)
				return null;

			var start = exclusiveStartKey != null ? exclusiveStartKey.CopyIf() : null;
			var iterable = start == null ? collection.find() : collection.find(Filters.gt("_id", start));
			iterable.projection(Projections.include("_id"));
			iterable.sort(Sorts.ascending("_id"));
			iterable.limit(proposeLimit);
			var lastKey = new OutObject<byte[]>();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					lastKey.value = key;
					callback.handle(key);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return lastKey.value != null ? ByteBuffer.Wrap(lastKey.value) : null;
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped)
				return null;

			var start = exclusiveStartKey != null ? exclusiveStartKey.CopyIf() : null;
			var iterable = start == null ? collection.find() : collection.find(Filters.lt("_id", start));
			iterable.limit(proposeLimit);
			iterable.sort(Sorts.descending("_id"));
			var lastKey = new OutObject<byte[]>();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					var value = getByteArray(document,"value");
					lastKey.value = key;
					callback.handle(key, value);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return lastKey.value != null ? ByteBuffer.Wrap(lastKey.value) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped)
				return null;

			var start = exclusiveStartKey != null ? exclusiveStartKey.CopyIf() : null;
			var iterable = start == null ? collection.find() : collection.find(Filters.lt("_id", start));
			iterable.projection(Projections.include("_id"));
			iterable.sort(Sorts.descending("_id"));
			iterable.limit(proposeLimit);
			var lastKey = new OutObject<byte[]>();
			iterable.forEach(document -> {
				try {
					var key = getByteArray(document, "_id");
					lastKey.value = key;
					callback.handle(key);
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			});
			return lastKey.value != null ? ByteBuffer.Wrap(lastKey.value) : null;
		}

		public @NotNull String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public @NotNull Database getDatabase() {
			return DatabaseMongoDb.this;
		}

		@Override
		public void close() {

		}

		@Override
		public void drop() {
			if (dropped)
				return;
			try {
				dropped = true; // setup flag before real drop();
				collection.drop();
			} catch (Exception e) {
				dropped = false; // rollback;
				Task.forceThrow(e);
			}
		}

		@Override
		public long getSize() {
			return collection.countDocuments();
		}

		@Override
		public long getSizeApproximation() {
			return collection.estimatedDocumentCount();
		}
	}
}
