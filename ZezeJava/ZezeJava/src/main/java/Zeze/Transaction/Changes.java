package Zeze.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.LongHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Changes {
	private static final Logger logger = LogManager.getLogger(Changes.class);

	private final LongHashMap<LogBean> Beans = new LongHashMap<>(); // 收集日志时,记录所有Bean修改. key is Bean.ObjectId
	private final HashMap<TableKey, Record> Records = new HashMap<>(); // 收集记录的修改,以后需要序列化传输.
	public final IdentityHashMap<Table, HashSet<ChangeListener>> Listeners = new IdentityHashMap<>();
	// private Transaction transaction;

	public Changes(Transaction t) {
		// transaction = t;
		// 建立脏记录的表的监听者的快照，以后收集日志和通知监听者都使用这个快照，避免由于监听者发生变化造成收集和通知不一致。
		for (var ar : t.getAccessedRecords().values()) {
			if (ar.Dirty) {
				var listeners = ar.AtomicTupleRecord.Record.getTable().getChangeListenerMap().getListeners();
				if (!listeners.isEmpty())
					Listeners.putIfAbsent(ar.AtomicTupleRecord.Record.getTable(), listeners);
			}
		}
	}

	public LongHashMap<LogBean> getBeans() {
		return Beans;
	}

	public HashMap<TableKey, Record> getRecords() {
		return Records;
	}

	public static final class Record {
		public static final int Remove = 0;
		public static final int Put = 1;
		public static final int Edit = 2;

		private int State;
		private Bean Value;
		private final HashSet<LogBean> LogBean = new HashSet<>();
		// 所有的日志修改树，key is Record.Value。不会被序列化。
		private final IdentityHashMap<Bean, LogBean> LogBeans = new IdentityHashMap<>();
		public final Table Table;

		public LogBean getLogBean() {
			var it = LogBean.iterator();
			return it.hasNext() ? it.next() : null;
		}

		public Log getVariableLog(int variableId) {
			var logBean = getLogBean();
			return logBean != null ? logBean.getVariables().get(variableId) : null;
		}

		public int getState() {
			return State;
		}

		public Bean getValue() {
			return Value;
		}

		public IdentityHashMap<Bean, LogBean> getLogBeans() {
			return LogBeans;
		}

		public Record(Table table) {
			Table = table;
		}

		public void Collect(RecordAccessed ar) {
			if (ar.CommittedPutLog != null) { // put or remove
				var put = ar.CommittedPutLog.getValue();
				if (null != put) {
					Value = put; // put
					State = Put;
				} else {
					Value = ar.AtomicTupleRecord.StrongRef; // old
					State = Remove;
				}
				return;
			}

			State = Edit;
			var logBean = LogBeans.get(ar.AtomicTupleRecord.StrongRef);
			if (logBean != null) {
				Value = ar.AtomicTupleRecord.StrongRef; // old
				LogBean.add(logBean); // edit
			}
		}

		public void Encode(ByteBuffer bb) {
			bb.WriteInt(State);
			switch (State) {
			case Remove:
				break;
			case Put:
				Value.Encode(bb);
				break;
			case Edit:
				bb.Encode(LogBean);
				break;
			}
		}

		public void Decode(ByteBuffer bb) {
			State = bb.ReadInt();
			switch (State) {
			case Remove:
				break;
			case Put:
				Value = Table.NewValue();
				Value.Decode(bb);
				break;
			case Edit:
				bb.Decode(LogBean, LogBean::new);
				break;
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			sb.append("State=").append(State).append(" PutValue=").append(Value);
			sb.append("\nLog=");
			ByteBuffer.BuildSortedString(sb, LogBean);
			sb.append("\nAllLog=");
			ByteBuffer.BuildSortedString(sb, LogBeans.values());
			return sb.toString();
		}
	}

	public void Collect(Bean recent, Log log) {
		// is table has listener
		if (null == Listeners.get(recent.rootInfo.getRecord().getTable()))
			return;

		Bean belong = log.getBelong();
		if (belong == null) {
			// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
			var r = Records.get(recent.tableKey());
			if (r == null) {
				r = new Record(recent.rootInfo.getRecord().getTable());
				Records.put(recent.tableKey(), r);
			}
			r.getLogBeans().put(recent, (LogBean)log);
			return; // root
		}

		var logBean = Beans.get(belong.objectId());
		if (logBean == null) {
			if (belong instanceof Collection) {
				// 容器使用共享的日志。需要先去查询，没有的话才创建。
				//noinspection ConstantConditions
				logBean = (LogBean)Transaction.getCurrent().GetLog(
						belong.parent().objectId() + belong.variableId());
			}
			if (logBean == null)
				logBean = belong.createLogBean();
			Beans.put(belong.objectId(), logBean);
		}
		logBean.Collect(this, belong, log);
	}

	public void CollectRecord(RecordAccessed ar) {
		// is table has listener
		if (null == Listeners.get(ar.AtomicTupleRecord.Record.getTable()))
			return;

		var tkey = ar.tableKey();
		var r = Records.get(tkey);
		if (r == null) {
			// put record only
			r = new Record(ar.AtomicTupleRecord.Record.getTable());
			Records.put(tkey, r);
		}

		r.Collect(ar);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, Records);
		return sb.toString();
	}

	public void NotifyListener() {
		for (var e : Records.entrySet()) {
			var v = e.getValue();
			var listeners = Listeners.get(v.Table);
			if (listeners != null) {
				var k = e.getKey();
				for (var l : listeners) {
					try {
						l.OnChanged(k.getKey(), v);
					} catch (Throwable ex) {
						logger.error("NotifyListener exception:", ex);
					}
				}
			} else
				logger.error("Impossible! Record Log Exist But No Listener");
		}
	}
}
