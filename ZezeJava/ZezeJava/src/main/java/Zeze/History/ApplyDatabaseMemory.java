package Zeze.History;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IApplyDatabase 的内存实现：apply 数据保存在进程内内存表中，不持久化（游标恒为null）。
 */
public class ApplyDatabaseMemory implements IApplyDatabase {
	private final ConcurrentHashMap<String, ApplyTableMemory> tables = new ConcurrentHashMap<>();
	// 当前打开的记录级事务。apply在ApplyHelper锁内单线程驱动，同一时刻至多一个。
	private IApplyRecordTxn activeRecordTxn;
	// 记录级事务的驱动线程（hist-03断言式守卫）：activeRecordTxn活跃期间，其他线程经
	// IApplyTable的put/remove会被静默路由进该事务（commit时随别人一起生效/回滚）。
	// begin时记录驱动线程，路由处核对当前线程——fail-fast暴露并发误用。非同步字段：
	// 断言式防御不引入锁，最坏漏检（可见性），不误报。
	private Thread recordTxnOwner;

	@Override
	public @NotNull IApplyTable open(@NotNull String tableName) {
		return tables.computeIfAbsent(tableName, ApplyTableMemory::new);
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

	/**
	 * 记录级事务（内存实现）：put/remove先进暂存Map（表→键→操作），记录内全部entry
	 * 成功后由commit一次性合并进真实表；任一entry异常rollback直接丢弃暂存——
	 * 单条tHistory记录内不存在“部分落库”的中间状态，重试时整条从干净状态重放。
	 */
	private class RecordTxn implements IApplyRecordTxn {
		// 暂存：表名→(键→新值；value为null表示remove)。LinkedHashMap保持entry应用顺序，
		// 同键后写覆盖先写（一条记录内changes以BTableKey为键本就唯一，防御性保留最后语义）。
		private final Map<String, LinkedHashMap<Binary, Binary>> staged = new HashMap<>();
		private boolean finished;

		@Override
		public void put(@NotNull String tableName, @NotNull Binary key, @NotNull Binary value) {
			staged.computeIfAbsent(tableName, __ -> new LinkedHashMap<>()).put(key, value);
		}

		@Override
		public void remove(@NotNull String tableName, @NotNull Binary key) {
			staged.computeIfAbsent(tableName, __ -> new LinkedHashMap<>()).put(key, null);
		}

		@Override
		public void commit() {
			if (finished)
				return;
			try {
				// 一次性合并进真实表：走到这里说明记录内全部entry已成功
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
			// 丢弃暂存：记录内任一entry异常，前缀entry的未提交写入全部作废
			if (finished)
				return;
			finish();
		}

		@Override
		public void close() {
			rollback(); // 幂等：未commit则按回滚收尾
		}

		private void finish() {
			finished = true;
			if (activeRecordTxn == this) {
				activeRecordTxn = null;
				recordTxnOwner = null;
			}
		}
	}

	// 非static内部类：put/remove需路由到外层库的activeRecordTxn
	private class ApplyTableMemory implements IApplyTable {
		private final @NotNull String tableName;
		private final ConcurrentHashMap<Binary, Binary> dataMap = new ConcurrentHashMap<>();

		public ApplyTableMemory(@NotNull String tableName) {
			this.tableName = tableName;
		}

		@Override
		public @NotNull String getTableName() {
			return tableName;
		}

		@Override
		public @Nullable Binary get(byte @NotNull [] key, int offset, int length) {
			// 事务内get仍读真实表：记录内changes按BTableKey唯一，同键不存在先写后读，
			// 跨键读取与暂存互不影响；同键读写一致性由ApplyTable的LRU保证。
			return dataMap.get(new Binary(key, offset, length));
		}

		@Override
		public void put(byte @NotNull [] key, int keyOffset, int keyLength,
						byte @NotNull [] value, int valueOffset, int valueLength) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				// 单线程契约守卫：事务内写入只应来自驱动线程，其他线程的写入立即暴露而非静默错挂
				checkRecordTxnOwner();
				// 记录级事务内：写入暂存，等记录内全部entry成功后由commit一次性合并
				recordTxn.put(tableName, new Binary(key, keyOffset, keyLength),
						new Binary(value, valueOffset, valueLength));
				return;
			}
			dataMap.put(new Binary(key, keyOffset, keyLength), new Binary(value, valueOffset, valueLength));
		}

		@Override
		public void remove(byte @NotNull [] key, int offset, int length) throws Exception {
			var recordTxn = activeRecordTxn;
			if (recordTxn != null) {
				checkRecordTxnOwner();
				// 记录级事务内：暂存remove标记，commit时才真正移除
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
		public void walk(@NotNull TableWalkHandleRaw walker) throws Exception {
			for (var e : dataMap.entrySet()) {
				if (!walker.handle(e.getKey().copyIf(), e.getValue().copyIf()))
					break;
			}
		}
	}
}
