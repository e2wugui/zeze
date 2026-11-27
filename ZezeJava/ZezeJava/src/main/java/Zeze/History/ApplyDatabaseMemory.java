package Zeze.History;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;
import org.jspecify.annotations.Nullable;

/**
 * IApplyDatabase 的内存实现：apply 数据保存在进程内内存表中，不持久化（游标恒为null）。
 */
public class ApplyDatabaseMemory implements IApplyDatabase {
	private final ConcurrentHashMap<String, ApplyTableMemory> tables = new ConcurrentHashMap<>();
	// 当前打开的记录级事务及驱动线程；apply 在 ApplyHelper 锁内单线程驱动，用于 fail-fast 检测并发误用。
	private @Nullable IApplyRecordTxn activeRecordTxn;
	private @Nullable Thread recordTxnOwner;

	@Override
	public IApplyTable open(String tableName) {
		return tables.computeIfAbsent(tableName, ApplyTableMemory::new);
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

	/**
	 * 记录级事务内存实现：写入先暂存，commit 时一次性合并至内存表，rollback 直接丢弃。
	 */
	private class RecordTxn implements IApplyRecordTxn {
		// 表名 -> (键 -> 新值；null 表示 remove)，按应用顺序暂存。
		private final Map<String, LinkedHashMap<Binary, @Nullable Binary>> staged = new HashMap<>();
		private boolean finished;

		@Override
		public void put(String tableName, Binary key, Binary value) {
			staged.computeIfAbsent(tableName, __ -> new LinkedHashMap<>()).put(key, value);
		}

		@Override
		public void remove(String tableName, Binary key) {
			staged.computeIfAbsent(tableName, __ -> new LinkedHashMap<>()).put(key, null);
		}

		@Override
		public void commit() {
			if (finished)
				return;
			try {
				for (var tableStaged : staged.entrySet()) {
					var table = tables.computeIfAbsent(tableStaged.getKey(), ApplyTableMemory::new);
					for (var e : tableStaged.getValue().entrySet()) {
						if (e.getValue() != null)
							table.dataMap.put(e.getKey(), e.getValue());
						else
							table.dataMap.remove(e.getKey());
					}
				}
			} finally {
				finish();
			}
		}

		@Override
		public void rollback() {
			if (finished)
				return;
			finish();
		}

		@Override
		public void close() {
			rollback();
		}

		private void finish() {
			finished = true;
			if (activeRecordTxn == this) {
				activeRecordTxn = null;
				recordTxnOwner = null;
			}
		}
	}

	// 非 static 内部类，便于 put/remove 路由至外层库的 activeRecordTxn。
	private class ApplyTableMemory implements IApplyTable {
		private final String tableName;
		private final ConcurrentHashMap<Binary, Binary> dataMap = new ConcurrentHashMap<>();

		public ApplyTableMemory(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public String getTableName() {
			return tableName;
		}

		@Override
		public @Nullable Binary get(byte [] key, int offset, int length) {
			// 单条记录内按 BTableKey 唯一，同键读写一致性由 ApplyTable 的 LRU 保证，此处直接读真实表。
			return dataMap.get(new Binary(key, offset, length));
		}

		@Override
		public void put(byte [] key, int keyOffset, int keyLength,
						byte [] value, int valueOffset, int valueLength) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				checkRecordTxnOwner();
				recordTxn.put(tableName, new Binary(key, keyOffset, keyLength),
						new Binary(value, valueOffset, valueLength));
				return;
			}
			dataMap.put(new Binary(key, keyOffset, keyLength), new Binary(value, valueOffset, valueLength));
		}

		@Override
		public void remove(byte [] key, int offset, int length) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				checkRecordTxnOwner();
				recordTxn.remove(tableName, new Binary(key, offset, length));
				return;
			}
			dataMap.remove(new Binary(key, offset, length));
		}

		@Override
		public boolean isEmpty() {
			return dataMap.isEmpty();
		}

		@Override
		public void walk(TableWalkHandleRaw walker) throws Exception {
			for (var e : dataMap.entrySet()) {
				if (!walker.handle(e.getKey().copyIf(), e.getValue().copyIf()))
					break;
			}
		}
	}
}
