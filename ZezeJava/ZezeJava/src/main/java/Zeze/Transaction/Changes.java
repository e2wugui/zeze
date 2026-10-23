package Zeze.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BiConsumer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.Map2ListenerView;
import Zeze.Util.LongHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 事务提交的变更收集器：汇总日志树与记录级变更（Put/Remove/Edit），
 * 用于通知 ChangeListener 与序列化传输（History/客户端同步）。
 */
public final class Changes {
	private static final Logger logger = LogManager.getLogger(Changes.class);

	// 无 History、无监听者的提交不创建 Changes；有消费者但无字段日志（如整条 Put）
	// 时仍无须分配 beans。getBeans 保持返回非空容器的契约。
	private @Nullable LogBeanCache beans; // 收集日志时,记录所有Bean修改. key is Bean.ObjectId
	private final HashMap<TableKey, Record> records = new HashMap<>(); // 收集记录的修改,以后需要序列化传输.
	private final @Nullable IdentityHashMap<Table, Set<ChangeListener>> listeners;
	private final boolean collectAll;
	private final Savepoint finalSavepoint;

	Changes(boolean collectAll,
			@Nullable IdentityHashMap<Table, Set<ChangeListener>> listeners, Savepoint finalSavepoint) {
		this.collectAll = collectAll;
		this.listeners = listeners;
		this.finalSavepoint = finalSavepoint;
	}

	/** 先确认脏写，再决定是否全量采集。纯读、无监听器的普通提交不分配 Changes。 */
	static @Nullable Changes createIfNeeded(Transaction t, boolean collectAll) {
		IdentityHashMap<Table, Set<ChangeListener>> snapshot = null;
		boolean hasDirty = false;
		for (var ar : t.getAccessedRecords().values()) {
			if (!ar.dirty)
				continue;
			hasDirty = true;
			var table = ar.atomicTupleRecord.record().getTable();
			var listeners = table.getChangeListenerMap().getListeners();
			if (!listeners.isEmpty()) {
				if (snapshot == null)
					snapshot = new IdentityHashMap<>();
				snapshot.putIfAbsent(table, listeners);
			}
		}
		return (collectAll && hasDirty) || snapshot != null ? new Changes(collectAll, snapshot, t.lastSavepoint()) : null;
	}

	public LongHashMap<LogBean> getBeans() {
		return getBeanCache().asMap();
	}

	private LogBeanCache getBeanCache() {
		var b = beans;
		return b != null ? b : (beans = new LogBeanCache());
	}

	public HashMap<TableKey, Record> getRecords() {
		return records;
	}

	/** 只创建并缓存节点，向上挂接由 collect 完成；与保存点日志迭代次序无关。 */

	public LogBean getOrCreateLogBean(Bean source) {
		var nodes = getBeanCache();
		long id = source.objectId();
		var existing = nodes.get(id);
		if (existing != null) return existing;
		long key = source.changeLogKey();
		var finalLog = key != 0 ? finalSavepoint.getLog(key) : null;
		return nodes.putIfAbsent(id, source.createLogBean(finalLog));
	}

	public static final class Record {
		public static final int Remove = 0;
		public static final int Put = 1;
		public static final int Edit = 2;

		private final Table table;
		private final HashSet<LogBean> logBean = new HashSet<>();
		// 所有的日志修改树，key is Record.Value。不会被序列化。
		private final IdentityHashMap<Bean, LogBean> logBeans = new IdentityHashMap<>();
		private @Nullable Bean value;
		private int state;

		public Record(Table table) {
			this.table = table;
		}

		public @Nullable LogBean getLogBean() {
			var it = logBean.iterator();
			return it.hasNext() ? it.next() : null;
		}

		public @Nullable Log getVariableLog(int variableId) {
			var logBean = getLogBean();
			if (logBean != null) {
				var variables = logBean.getVariables();
				if (variables != null)
					return variables.get(variableId);
			}
			return null;
		}

		private Log sourceVariableLog(int variableId) {
			var root = getLogBean();
			if (root == null || root.getThis() == null)
				throw new IllegalStateException("listener projection requires a collected source record");
			var variables = root.getVariables();
			var log = variables != null ? variables.get(variableId) : null;
			if (log == null)
				throw new IllegalArgumentException("no change for variable " + variableId);
			return log;
		}

		@SuppressWarnings("unchecked")
		public <K, V extends Bean> Map2ListenerView<K, V> map2ListenerView(int variableId) {
			return Map2ListenerView.of((LogMap2<K, V>)sourceVariableLog(variableId));
		}

		@SuppressWarnings("unchecked")
		public <K extends Comparable<K>, V extends Bean> Map2ListenerView<K, V> sortedMap2ListenerView(int variableId) {
			return Map2ListenerView.of((LogSortedMap2<K, V>)sourceVariableLog(variableId));
		}

		@SuppressWarnings("unchecked")
		public <K, V extends Bean> void forEachMap2Upsert(int variableId, BiConsumer<K, V> action) {
			Map2ListenerView.forEachUpsert((LogMap2<K, V>)sourceVariableLog(variableId), action);
		}

		@SuppressWarnings("unchecked")
		public <K extends Comparable<K>, V extends Bean> void forEachSortedMap2Upsert(
			int variableId, BiConsumer<K, V> action) {
			Map2ListenerView.forEachUpsert((LogSortedMap2<K, V>)sourceVariableLog(variableId), action);
		}

		public IdentityHashMap<Bean, LogBean> getLogBeans() {
			return logBeans;
		}

		public @Nullable Bean getValue() {
			return value;
		}

		public int getState() {
			return state;
		}

		public Table getTable() {
			return table;
		}

		public void collect(RecordAccessed ar) {
			if (ar.committedPutLog != null) { // put or remove
				var put = ar.committedPutLog.getValue();
				if (put != null) {
					value = put; // put
					state = Put;
				} else {
					value = ar.atomicTupleRecord.strongRef(); // old
					state = Remove;
				}
				return;
			}

			state = Edit;
			var logBean = logBeans.get(ar.atomicTupleRecord.strongRef());
			if (logBean != null) {
				value = ar.atomicTupleRecord.strongRef(); // old
				this.logBean.add(logBean); // edit
			}
		}

		public void encode(ByteBuffer bb) {
			bb.WriteUInt(state);
			switch (state) {
			case Remove:
				break;
			case Put:
				//noinspection DataFlowIssue
				value.encode(bb);
				break;
			case Edit:
				bb.WriteUInt(logBean.size());
				for (var log : logBean)
					log.encode(bb);
				break;
			}
		}

		public void decode(ByteBuffer bb) {
			state = bb.ReadUInt();
			switch (state) {
			case Remove:
				break;
			case Put:
				value = table.newValue();
				value.decode(bb);
				break;
			case Edit:
				logBean.clear();
				for (int count = bb.ReadUInt(); count > 0; --count)
					logBean.add(LogBean.decode(0, bb));
				break;
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			sb.append("State=").append(state).append(" PutValue=").append(value);
			sb.append("\nLog=");
			ByteBuffer.BuildSortedString(sb, logBean);
			sb.append("\nAllLog=");
			ByteBuffer.BuildSortedString(sb, logBeans.values());
			return sb.toString();
		}
	}

	public boolean isInterested(Bean source) {
		if (collectAll)
			return true;
		var root = source.rootInfo;
		if (root == null || listeners == null)
			return false;
		var record = root.record();
		assert record != null;
		return listeners.containsKey(record.getTable());
	}

	public void collect(Bean source, Log child) {
		if (!isInterested(source)) return;
		var node = getOrCreateLogBean(source);
		if (node.addChild(source, child)) attach(source, node);
	}

	public void collectContainer(Bean source) {
		if (isInterested(source)) attach(source, getOrCreateLogBean(source));
	}

	private void attach(Bean source, LogBean node) {
		var parent = source.parent();
		if (parent != null) {
			collect(parent, node);
			return;
		}
		var root = source.rootInfo;
		assert root != null;
		var key = root.tableKey();
		var record = records.get(key);
		if (record == null) {
			var sourceRecord = root.record();
			assert sourceRecord != null;
			records.put(key, record = new Record(sourceRecord.getTable()));
		}
		record.logBeans.put(source, node);
	}

	public void collectRecord(RecordAccessed ar) {
		// is table has listener
		if (!collectAll && (listeners == null || listeners.get(ar.atomicTupleRecord.record().getTable()) == null))
			return;

		var tkey = ar.tableKey();
		var r = records.get(tkey);
		if (r == null) {
			// put record only
			r = new Record(ar.atomicTupleRecord.record().getTable());
			//noinspection DataFlowIssue
			records.put(tkey, r);
		}

		r.collect(ar);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, records);
		return sb.toString();
	}

	public void notifyListener() {
		var ls = listeners;
		if (ls == null)
			return;
		for (var e : records.entrySet()) {
			var v = e.getValue();
			var listeners = ls.get(v.table);
			if (listeners != null) {
				var k = e.getKey();
				for (var l : listeners) {
					try {
						l.OnChanged(k.getKey(), v);
					} catch (Throwable ex) { // logger.error
						// run handle.
						logger.error("NotifyListener exception:", ex);
					}
				}
			}
			// 这里面记录的日志量可能比较多，超过实际使用，包含了废弃的。
			// 通知完以后不再需要。
			// 【注意】history引用了其他部分，不能清除。
			// help gc
			v.logBeans.clear();
		}
	}

	void finishCollect() {
		if (beans != null) beans.finishCollect();
	}
}
