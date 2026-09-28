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
	private final Database.AbstractKVTable cursorStorage;
	private final ConcurrentHashMap<String, ApplyTableZeze> tables = new ConcurrentHashMap<>();
	// 当前打开的记录级事务。apply在ApplyHelper锁内单线程驱动，同一时刻至多一个。
	private IApplyRecordTxn activeRecordTxn;
	// 记录级事务的驱动线程（hist-03断言式守卫）：activeRecordTxn活跃期间，其他线程经
	// IApplyTable的put/remove会被静默路由进该事务的底层dbTxn（commit时随别人一起生效/回滚）。
	// begin时记录驱动线程，路由处核对当前线程——fail-fast暴露并发误用。非同步字段：
	// 断言式防御不引入锁，最坏漏检（可见性），不误报。
	private Thread recordTxnOwner;

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
		recordTxnOwner = Thread.currentThread();
		return txn;
	}

	// 单线程契约守卫（见recordTxnOwner注释）：路由进活跃记录级事务的写入只应来自驱动线程。
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
				rollback(); // 契约：不抛出，不掩盖原始提交异常
				throw Task.forceThrow(ex);
			}
			// commit 已成功：数据与持久游标均已生效，此后任何失败都不得让调用方按未提交处理
			// （重试整条重放，Edit 非幂等会叠加重复元素）；close 失败仅记录。
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
				// 契约（IApplyRecordTxn）：不抛出，不得掩盖正在传播的原始 apply 异常；
				// 回滚失败按回滚完成收尾，写入撤销与否不确定，由 ApplyHelper.finally 的
				// LRU 失效兜底（否则重试命中脏缓存，等价对已回滚记录二次应用）。
				logger.warn("record txn rollback failed", e);
			} finally {
				closeQuietly();
				finish();
			}
		}

		@Override
		public void close() {
			rollback(); // 幂等：未commit则按回滚收尾
		}

		// dbTxn.close() 不能抛出：回滚路径会掩盖正在传播的原始 apply 异常；提交成功路径
		// close 失败不得让调用方按未提交处理。失败仅记录，后果限于底层资源清理。
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
				// 单线程契约守卫：事务内写入只应来自驱动线程，其他线程的写入立即暴露而非静默错挂
				checkRecordTxnOwner();
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
				checkRecordTxnOwner();
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
