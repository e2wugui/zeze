package Zeze.History;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Util.Id128;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * IApplyDatabase 的 Zeze Database 实现：apply 数据与回放游标持久化到独立的 KV 库。
 */
public class ApplyDatabaseZeze implements IApplyDatabase {
	private static final Logger logger = LogManager.getLogger(ApplyDatabaseZeze.class);

	// 游标持久化的伪表与固定键——库独立于业务表的命名空间，经openTable直开（不要求在zeze注册）。
	private static final String cursorTableName = "__cursor__";
	private static final byte[] cursorKeyBytes = {0};

	private static ByteBuffer cursorKey() {
		return ByteBuffer.Wrap(cursorKeyBytes); // 每次新建，避免共享可变ByteBuffer被底层后端移动写指针
	}

	private final Database dbForApply;
	private final String applyDbName;
	private final Database.AbstractKVTable cursorStorage;
	private final ConcurrentHashMap<String, ApplyTableZeze> tables = new ConcurrentHashMap<>();
	// 当前打开的记录级事务及驱动线程；apply 在 ApplyHelper 锁内单线程驱动，用于 fail-fast 检测并发误用。
	private @Nullable IApplyRecordTxn activeRecordTxn;
	private @Nullable Thread recordTxnOwner;

	public ApplyDatabaseZeze(Application zeze, String applyDbName) {
		if (applyDbName.isBlank())
			throw new RuntimeException("apply database must have a name.");
		dbForApply = zeze.getDatabase(applyDbName);
		this.applyDbName = applyDbName;
		// apply 库必须独立于业务表所在库，防止回放写入与游标覆盖业务表数据；后注册表在 ApplyTableZeze 构造期兜底校验。
		for (var t : zeze.getTables().values()) {
			if (isApplyDatabase(t.getDatabase()))
				throw new RuntimeException("apply database config conflict: '" + applyDbName
						+ "' is also the database of business table '" + t.getName()
						+ "'. apply data and replay cursor must persist to a separate kv database.");
		}
		cursorStorage = openReadyTable(cursorTableName, Bean.hash32(cursorTableName));
	}

	private Database.AbstractKVTable openReadyTable(String tableName, int tableId) {
		var storage = dbForApply.openTable(tableName, tableId);
		try {
			if (!(storage instanceof Database.AbstractKVTable kvStorage))
				throw new IllegalStateException("apply database '" + applyDbName
						+ "' requires a KV table: " + tableName);
			kvStorage.waitReady();
			return kvStorage;
		} catch (Throwable failure) {
			try {
				storage.close();
			} catch (Throwable cleanup) {
				if (cleanup != failure)
					failure.addSuppressed(cleanup);
			}
			throw failure;
		}
	}

	private boolean isApplyDatabase(@Nullable Database database) {
		// Memory后端按URL共享静态命名空间，配置名不同并不隔离物理表。
		// 其他后端的URL之外还可能有schema/用户等配置，此处只拒绝已能证明的同库情形。
		return database == dbForApply || database instanceof DatabaseMemory
				&& dbForApply instanceof DatabaseMemory
				&& database.getDatabaseUrl().equals(dbForApply.getDatabaseUrl());
	}

	@Override
	public IApplyTable open(String tableName) {
		return tables.computeIfAbsent(tableName, (key) -> new ApplyTableZeze(tableName));
	}

	@Override
	public IApplyRecordTxn beginRecordTxn() {
		if (activeRecordTxn != null)
			throw new IllegalStateException("record txn already begun.");
		var txn = new RecordTxn();
		activeRecordTxn = txn;
		recordTxnOwner = Thread.currentThread();
		return txn;
	}

	private void checkRecordTxnOwner() {
		if (Thread.currentThread() != recordTxnOwner)
			throw new IllegalStateException("apply record txn is driven by a single thread: concurrent "
					+ "put/remove would be routed into the active record txn of another thread. owner="
					+ recordTxnOwner + ", current=" + Thread.currentThread());
	}

	@Override
	public @Nullable Id128 loadCursor() {
		var value = cursorStorage.find(cursorKey());
		if (null == value)
			return null;
		var id = Id128.readFrom(value);
		return id;
	}

	@Override
	public void saveCursor(Id128 key, IApplyRecordTxn txn) {
		// 游标与记录数据共用底层事务提交，保证数据与进度原子一致。
		if (!(txn instanceof RecordTxn recordTxn) || txn != activeRecordTxn)
			throw new IllegalStateException("saveCursor requires the active RecordTxn of this database.");
		checkRecordTxnOwner();
		var value = ByteBuffer.Allocate();
		key.encode(value);
		cursorStorage.replace(recordTxn.dbTxn, cursorKey(), value);
	}

	/**
	 * 记录级事务 Zeze 实现：单条 tHistory 记录内的全部写入共用同一底层 Database.Transaction 原子提交或回滚。
	 */
	private class RecordTxn implements IApplyRecordTxn {
		private final Database.Transaction dbTxn = dbForApply.beginTransaction();
		private boolean finished;

		@Override
		public void put(String tableName, Binary key, Binary value) {
			tables.computeIfAbsent(tableName, ApplyTableZeze::new).storage
					.replace(dbTxn, ByteBuffer.Wrap(key), ByteBuffer.Wrap(value));
		}

		@Override
		public void remove(String tableName, Binary key) {
			tables.computeIfAbsent(tableName, ApplyTableZeze::new).storage
					.remove(dbTxn, ByteBuffer.Wrap(key));
		}

		@Override
		public void commit() {
			if (finished)
				return;
			try {
				dbTxn.commit();
			} catch (Exception ex) {
				rollback();
				throw Task.forceThrow(ex);
			}
			// 提交已生效，后续 close 失败不得上抛以免调用方误判为未提交而重复回放。
			finish();
			closeQuietly();
		}

		@Override
		public void rollback() {
			if (finished)
				return;
			try {
				dbTxn.rollback();
			} catch (Exception e) {
				// 不上抛以免掩盖原始 apply 异常；缓存一致性由调用方 LRU 失效兜底。
				logger.warn("record txn rollback failed", e);
			} finally {
				closeQuietly();
				finish();
			}
		}

		@Override
		public void close() {
			rollback();
		}

		private void closeQuietly() {
			try {
				dbTxn.close();
			} catch (Exception e) {
				logger.warn("record txn close failed", e);
			}
		}

		private void finish() {
			finished = true;
			if (activeRecordTxn == this) {
				activeRecordTxn = null;
				recordTxnOwner = null;
			}
		}
	}

	public class ApplyTableZeze implements IApplyTable {

		private final String tableName;
		private final Database.AbstractKVTable storage;

		public ApplyTableZeze(String tableName) {
			this.tableName = tableName;
			//noinspection DataFlowIssue
			var table = dbForApply.getZeze().getTable(tableName);
			if (null == table)
				throw new RuntimeException("table not exist in zeze. name=" + tableName);
			// 对晚于 ApplyDatabaseZeze 构造注册的业务表兜底校验库隔离。
			if (isApplyDatabase(table.getDatabase()))
				throw new RuntimeException("apply table config conflict: business table '" + tableName
						+ "' is hosted in the apply database '" + applyDbName
						+ "'. apply data and replay cursor must persist to a separate kv database.");
			storage = openReadyTable(tableName, table.getId());
		}

		@Override
		public String getTableName() {
			return tableName;
		}

		@Override
		public @Nullable Binary get(byte [] key, int offset, int length) {
			var value = storage.find(ByteBuffer.Wrap(key, offset, length));
			if (null == value)
				return null;
			return new Binary(value);
		}

		@Override
		public void put(byte [] key, int keyOffset, int keyLength, byte [] value, int valueOffset, int valueLength) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				checkRecordTxnOwner();
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
		public void remove(byte [] key, int offset, int length) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				checkRecordTxnOwner();
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
			return Objects.requireNonNull(empty.value);
		}

		@Override
		public void walk(TableWalkHandleRaw walker) throws Exception {
			storage.walk(walker);
		}
	}
}
