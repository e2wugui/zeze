package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyDatabaseZeze implements IApplyDatabase {
	private final Database dbForApply;
	private final ConcurrentHashMap<String, ApplyTableZeze> tables = new ConcurrentHashMap<>();

	public ApplyDatabaseZeze(@NotNull Application zeze, @NotNull String applyDbName) {
		if (applyDbName.isBlank())
			throw new RuntimeException("apply database must have a name.");
		dbForApply = zeze.getDatabase(applyDbName);
	}

	@Override
	public @NotNull IApplyTable open(@NotNull String tableName) {
		return tables.computeIfAbsent(tableName, (key) -> new ApplyTableZeze(tableName));
	}

	public class ApplyTableZeze implements IApplyTable {

		private final String tableName;
		private final Database.AbstractKVTable storage;

		public ApplyTableZeze(@NotNull String tableName) {
			this.tableName = tableName;
			var table = dbForApply.getZeze().getTable(tableName);
			if (null == table)
				throw new RuntimeException("table not exist in zeze. name=" + tableName);
			var storage = dbForApply.openTable(tableName, table.getId());
			if (!(storage instanceof Database.AbstractKVTable))
				throw new RuntimeException("apply table need a kv-table.");
			this.storage = (Database.AbstractKVTable)storage;
		}

		@Override
		public @NotNull String getTableName() {
			return tableName;
		}

		@Override
		public @Nullable Binary get(byte @NotNull [] key, int offset, int length) {
			var value = storage.find(ByteBuffer.Wrap(key, offset, length));
			if (null == value)
				return null;
			return new Binary(value);
		}

		@Override
		public void put(byte @NotNull [] key, int keyOffset, int keyLength, byte @NotNull [] value, int valueOffset, int valueLength) throws Exception {
			var txn = dbForApply.beginTransaction();
			try {
				storage.replace(txn, ByteBuffer.Wrap(key, keyOffset, keyLength), ByteBuffer.Wrap(value, valueOffset, valueLength));
				txn.commit();
			} catch (Exception ex) {
				txn.rollback();
				Task.forceThrow(ex);
			} finally {
				txn.close();
			}
		}

		@Override
		public void remove(byte @NotNull [] key, int offset, int length) throws Exception {
			var txn = dbForApply.beginTransaction();
			try {
				storage.remove(txn, ByteBuffer.Wrap(key, offset, length));
				txn.commit();
			} catch (Exception ex) {
				txn.rollback();
				Task.forceThrow(ex);
			} finally {
				txn.close();
			}
		}

		/**
		 * 使用walk精确得到，这个用来验证apply用，一般来说表基本是空的。
		 * @return true if empty.
		 */
		@Override
		public boolean isEmpty() throws Exception {
			var empty = new OutObject<>(true);
			storage.walkKey(null, 1, (rawKey) -> {
				empty.value = false;
				return false;
			});
			return empty.value;
		}

		@Override
		public void walk(@NotNull TableWalkHandleRaw walker) throws Exception {
			storage.walk(walker);
		}
	}
}
