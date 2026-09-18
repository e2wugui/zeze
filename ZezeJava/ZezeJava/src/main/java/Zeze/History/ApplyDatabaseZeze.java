package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Util.Id128;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyDatabaseZeze implements IApplyDatabase {
	private static final Logger logger = LogManager.getLogger(ApplyDatabaseZeze.class);

	// FND8-28：游标持久化的伪表与固定键——库独立于业务表的命名空间，经openTable直开
	// （不要求在zeze注册，对齐OperatesDynamoDb的schema表先例）。
	private static final String cursorTableName = "__cursor__";
	private static final byte[] cursorKeyBytes = {0};

	private static ByteBuffer cursorKey() {
		return ByteBuffer.Wrap(cursorKeyBytes); // 每次新建，避免共享可变ByteBuffer被底层后端移动写指针
	}

	private final Database dbForApply;
	private final Database.AbstractKVTable cursorStorage;
	private final ConcurrentHashMap<String, ApplyTableZeze> tables = new ConcurrentHashMap<>();
	// 当前打开的记录级事务。apply在ApplyHelper锁内单线程驱动，同一时刻至多一个。
	private IApplyRecordTxn activeRecordTxn;

	public ApplyDatabaseZeze(@NotNull Application zeze, @NotNull String applyDbName) {
		if (applyDbName.isBlank())
			throw new RuntimeException("apply database must have a name.");
		dbForApply = zeze.getDatabase(applyDbName);
		var storage = dbForApply.openTable(cursorTableName, Bean.hash32(cursorTableName));
		if (!(storage instanceof Database.AbstractKVTable kvStorage))
			throw new RuntimeException("apply database need a kv-table for cursor. name=" + applyDbName);
		cursorStorage = kvStorage;
	}

	@Override
	public @NotNull IApplyTable open(@NotNull String tableName) {
		return tables.computeIfAbsent(tableName, (key) -> new ApplyTableZeze(tableName));
	}

	@Override
	public @NotNull IApplyRecordTxn beginRecordTxn() {
		if (activeRecordTxn != null)
			throw new IllegalStateException("record txn already begun."); // 防御：apply单线程，不应嵌套/泄漏
		var txn = new RecordTxn();
		activeRecordTxn = txn;
		return txn;
	}

	@Override
	public @Nullable Id128 loadCursor() {
		var value = cursorStorage.find(cursorKey());
		if (null == value)
			return null;
		var id = new Id128();
		id.decode(value);
		return id;
	}

	@Override
	public void saveCursor(@NotNull Id128 key, @NotNull IApplyRecordTxn txn) throws Exception {
		// 必须走当前记录级事务的底层dbTxn：游标与记录数据同事务提交，杜绝
		// "数据持久而进度丢失"的错配（重启后游标归零整段重放，Edit非幂等即污染）。
		if (!(txn instanceof RecordTxn recordTxn))
			throw new IllegalStateException("saveCursor requires the RecordTxn of this database.");
		var value = ByteBuffer.Allocate();
		key.encode(value);
		cursorStorage.replace(recordTxn.dbTxn, cursorKey(), value);
	}

	/**
	 * 记录级事务（Zeze实现）：put/remove本就是每次独立开事务逐条提交（见ApplyTableZeze），
	 * 并非天然原子——第i个entry失败时前i-1个entry已各自提交落库。故记录级事务期间
	 * 全部写入共用同一个底层Database.Transaction，由commit统一提交，实现“单条tHistory
	 * 记录=原子单元”；任一entry异常rollback整个底层事务，前缀entry的写入一并撤销。
	 */
	private class RecordTxn implements IApplyRecordTxn {
		private final Database.Transaction dbTxn = dbForApply.beginTransaction();
		private boolean finished;

		@Override
		public void put(@NotNull String tableName, @NotNull Binary key, @NotNull Binary value) throws Exception {
			tables.computeIfAbsent(tableName, ApplyTableZeze::new).storage
					.replace(dbTxn, ByteBuffer.Wrap(key), ByteBuffer.Wrap(value));
		}

		@Override
		public void remove(@NotNull String tableName, @NotNull Binary key) throws Exception {
			tables.computeIfAbsent(tableName, ApplyTableZeze::new).storage
					.remove(dbTxn, ByteBuffer.Wrap(key));
		}

		@Override
		public void commit() throws Exception {
			if (finished)
				return;
			try {
				dbTxn.commit();
			} catch (Exception ex) {
				dbTxn.rollback();
				throw Task.forceThrow(ex);
			} finally {
				// commit成败都结束本事务；close失败按原样传播（此时无待保护的原始异常）
				dbTxn.close();
				finish();
			}
		}

		@Override
		public void rollback() {
			if (finished)
				return;
			try {
				dbTxn.rollback();
			} finally {
				closeQuietly();
				finish();
			}
		}

		@Override
		public void close() {
			rollback(); // 幂等：未commit则按回滚收尾
		}

		// 回滚路径中的dbTxn.close()不能抛出（会掩盖正在传播的原始apply异常），失败仅记录；
		// 事务此时已回滚，关闭失败的后果限于底层资源清理。
		private void closeQuietly() {
			try {
				dbTxn.close();
			} catch (Exception e) {
				logger.warn("record txn close failed after rollback", e);
			}
		}

		private void finish() {
			finished = true;
			if (activeRecordTxn == this)
				activeRecordTxn = null;
		}
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
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				// 记录级事务内：写入共享底层事务，由RecordTxn.commit统一提交，保证单条记录原子
				recordTxn.put(tableName, new Binary(key, keyOffset, keyLength),
						new Binary(value, valueOffset, valueLength));
				return;
			}
			var txn = dbForApply.beginTransaction();
			try {
				storage.replace(txn, ByteBuffer.Wrap(key, keyOffset, keyLength), ByteBuffer.Wrap(value, valueOffset, valueLength));
				txn.commit();
			} catch (Exception ex) {
				txn.rollback();
				throw Task.forceThrow(ex);
			} finally {
				txn.close();
			}
		}

		@Override
		public void remove(byte @NotNull [] key, int offset, int length) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				// 记录级事务内：写入共享底层事务，由RecordTxn.rollback可整体撤销
				recordTxn.remove(tableName, new Binary(key, offset, length));
				return;
			}
			var txn = dbForApply.beginTransaction();
			try {
				storage.remove(txn, ByteBuffer.Wrap(key, offset, length));
				txn.commit();
			} catch (Exception ex) {
				txn.rollback();
				throw Task.forceThrow(ex);
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
