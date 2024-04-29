package Zeze.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.LongHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Changes {
	private static final Logger logger = LogManager.getLogger(Changes.class);

	private final LongHashMap<LogBean> beans = new LongHashMap<>(); // 收集日志时,记录所有Bean修改. key is Bean.ObjectId
	private final HashMap<@NotNull TableKey, @NotNull Record> records = new HashMap<>(); // 收集记录的修改,以后需要序列化传输.
	private final IdentityHashMap<@NotNull Table, @NotNull Set<@NotNull ChangeListener>> listeners = new IdentityHashMap<>();
	// private Transaction transaction;

	public Changes(@NotNull Transaction t) {
		// transaction = t;
		// 建立脏记录的表的监听者的快照，以后收集日志和通知监听者都使用这个快照，避免由于监听者发生变化造成收集和通知不一致。
		for (var ar : t.getAccessedRecords().values()) {
			if (ar.dirty) {
				var listeners = ar.atomicTupleRecord.record.getTable().getChangeListenerMap().getListeners();
				if (!listeners.isEmpty())
					this.listeners.putIfAbsent(ar.atomicTupleRecord.record.getTable(), listeners);
			}
		}
	}

	public @NotNull LongHashMap<LogBean> getBeans() {
		return beans;
	}

	public @NotNull HashMap<@NotNull TableKey, @NotNull Record> getRecords() {
		return records;
	}

	public static final class Record {
		public static final int Remove = 0;
		public static final int Put = 1;
		public static final int Edit = 2;

		private final Table table;
		private final HashSet<LogBean> logBean = new HashSet<>();
		// 所有的日志修改树，key is Record.Value。不会被序列化。
		private final IdentityHashMap<Bean, LogBean> logBeans = new IdentityHashMap<>();
		private Bean value;
		private int state;

		public Record(@NotNull Table table) {
			this.table = table;
		}

		public @Nullable LogBean getLogBean() {
			var it = logBean.iterator();
			return it.hasNext() ? it.next() : null;
		}

		public @Nullable Log getVariableLog(int variableId) {
			var logBean = getLogBean();
			return logBean != null ? logBean.getVariables().get(variableId) : null;
		}

		public @NotNull IdentityHashMap<Bean, LogBean> getLogBeans() {
			return logBeans;
		}

		public Bean getValue() {
			return value;
		}

		public int getState() {
			return state;
		}

		public Table getTable() {
			return table;
		}

		public void collect(@NotNull RecordAccessed ar) {
			if (ar.committedPutLog != null) { // put or remove
				var put = ar.committedPutLog.getValue();
				if (null != put) {
					value = put; // put
					state = Put;
				} else {
					value = ar.atomicTupleRecord.strongRef; // old
					state = Remove;
				}
				return;
			}

			state = Edit;
			var logBean = logBeans.get(ar.atomicTupleRecord.strongRef);
			if (logBean != null) {
				value = ar.atomicTupleRecord.strongRef; // old
				this.logBean.add(logBean); // edit
			}
		}

		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(state);
			switch (state) {
			case Remove:
				break;
			case Put:
				value.encode(bb);
				break;
			case Edit:
				bb.encode(logBean);
				break;
			}
		}

		public void decode(@NotNull ByteBuffer bb) {
			state = bb.ReadInt();
			switch (state) {
			case Remove:
				break;
			case Put:
				value = table.newValue();
				value.decode(bb);
				break;
			case Edit:
				bb.decode(logBean, LogBean::new);
				break;
			}
		}

		@Override
		public @NotNull String toString() {
			var sb = new StringBuilder();
			sb.append("State=").append(state).append(" PutValue=").append(value);
			sb.append("\nLog=");
			ByteBuffer.BuildSortedString(sb, logBean);
			sb.append("\nAllLog=");
			ByteBuffer.BuildSortedString(sb, logBeans.values());
			return sb.toString();
		}
	}

	public void collect(@NotNull Bean recent, @NotNull Log log) {
		// is table has listener
		//noinspection DataFlowIssue
		if (null == listeners.get(recent.rootInfo.getRecord().getTable()))
			return;

		Bean belong = log.getBelong();
		if (belong == null) {
			// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
			var r = records.get(recent.tableKey());
			if (r == null) {
				r = new Record(recent.rootInfo.getRecord().getTable());
				//noinspection DataFlowIssue
				records.put(recent.tableKey(), r);
			}
			r.logBeans.put(recent, (LogBean)log);
			return; // root
		}

		var logBean = beans.get(belong.objectId());
		if (logBean == null) {
			if (belong instanceof Collection) {
				// 容器使用共享的日志。需要先去查询，没有的话才创建。
				//noinspection ConstantConditions
				logBean = (LogBean)Transaction.getCurrent().getLog(
						belong.parent().objectId() + belong.variableId());
			}
			if (logBean == null)
				logBean = belong.createLogBean();
			beans.put(belong.objectId(), logBean);
		}
		logBean.collect(this, belong, log);
	}

	public void collectRecord(@NotNull RecordAccessed ar) {
		// is table has listener
		if (null == listeners.get(ar.atomicTupleRecord.record.getTable()))
			return;

		var tkey = ar.tableKey();
		var r = records.get(tkey);
		if (r == null) {
			// put record only
			r = new Record(ar.atomicTupleRecord.record.getTable());
			//noinspection DataFlowIssue
			records.put(tkey, r);
		}

		r.collect(ar);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, records);
		return sb.toString();
	}

	public void notifyListener() {
		for (var e : records.entrySet()) {
			var v = e.getValue();
			var listeners = this.listeners.get(v.table);
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
			} else
				logger.error("Impossible! Record Log Exist But No Listener");
		}
	}
}
