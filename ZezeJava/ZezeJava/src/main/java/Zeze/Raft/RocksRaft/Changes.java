package Zeze.Raft.RocksRaft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashMap;

public final class Changes extends Zeze.Raft.Log {
	private final Rocks rocks;
	private final LongHashMap<LogBean> beans = new LongHashMap<>(); // 收集日志时,记录所有Bean修改. key is Bean.ObjectId
	private final HashMap<TableKey, Record> records = new HashMap<>(); // 收集记录的修改,以后需要序列化传输.
	private final IntHashMap<Long> atomicLongs = new IntHashMap<>();
	private Transaction transaction;
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(Changes.class.getName());

	@Override
	public long typeId() {
		return TypeId_;
	}

	public Changes(Rocks r) {
		super(null);
		rocks = r;
	}

	public Changes(Rocks r, Transaction t, IRaftRpc req) {
		super(req);
		rocks = r;
		transaction = t;
	}

	public Rocks getRocks() {
		return rocks;
	}

	public LongHashMap<LogBean> getBeans() {
		return beans;
	}

	public HashMap<TableKey, Record> getRecords() {
		return records;
	}

	public IntHashMap<Long> getAtomicLongs() {
		return atomicLongs;
	}

	public static final class Record {
		public static final int Remove = 0;
		public static final int Put = 1;
		public static final int Edit = 2;

		private int tableTemplateId;
		private String tableTemplateName;
		private int state;
		private Bean putValue;
		private final Set<LogBean> logBean = new HashSet<>();
		// 所有的日志修改树，key is Record.Value。不会被序列化。
		private final IdentityHashMap<Bean, LogBean> logBeans = new IdentityHashMap<>();
		public Table<?, ?> table;

		public void setTableTemplateId(int value) {
			tableTemplateId = value;
		}

		public String getTableTemplateName() {
			return tableTemplateName;
		}

		public void setTableTemplateName(String value) {
			tableTemplateName = value;
		}

		public int getState() {
			return state;
		}

		public Bean getPutValue() {
			return putValue;
		}

		public Set<LogBean> getLogBean() {
			return logBean;
		}

		public IdentityHashMap<Bean, LogBean> getLogBeans() {
			return logBeans;
		}

		public void setTableByName(Changes changes, String templateName) {
			var tableTpl = changes.rocks.getTableTemplate(templateName);
			if (tableTpl == null) {
				var names = new StringBuilder();
				for (String name : changes.rocks.getTableTemplates().keySet())
					names.append(' ').append(name);
				throw new IllegalStateException("unknown table template: " + templateName + ", available:" + names);
			}
			table = tableTpl.openTable(tableTemplateId);
		}

		public void collect(Transaction.RecordAccessed ar) {
			if (ar.getPutLog() != null) { // put or remove
				putValue = ar.getPutLog().value;
				state = putValue == null ? Remove : Put;
				return;
			}

			state = Edit;
			var logBean = logBeans.get(ar.getOrigin().getValue());
			if (logBean != null)
				this.logBean.add(logBean); // edit
		}

		public void encode(ByteBuffer bb) {
			bb.WriteInt(state);
			switch (state) {
			case Remove:
				break;
			case Put:
				putValue.encode(bb);
				break;
			case Edit:
				bb.encode(logBean);
				break;
			}
		}

		public void decode(IByteBuffer bb) {
			state = bb.ReadInt();
			switch (state) {
			case Remove:
				break;
			case Put:
				putValue = table.newValue();
				putValue.decode(bb);
				break;
			case Edit:
				bb.decode(logBean, LogBean::new);
				break;
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			sb.append("State=").append(state).append(" PutValue=").append(putValue);
			sb.append("\nLog=");
			ByteBuffer.BuildSortedString(sb, logBean);
			sb.append("\nAllLog=");
			ByteBuffer.BuildSortedString(sb, logBeans.values());
			return sb.toString();
		}
	}

	public void collect(Bean recent, Log log) {
		Bean belong = log.getBelong();
		if (belong == null) {
			// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
			var r = records.get(recent.tableKey());
			if (r == null) {
				r = new Record();
				var table = recent.rootInfo().getRecord().getTable();
				r.setTableTemplateId(table.getTemplateId());
				r.setTableTemplateName(table.getTemplateName());
				records.put(recent.tableKey(), r);
			}
			r.getLogBeans().put(recent, (LogBean)log);
			return; // root
		}

		var logBean = beans.get(belong.objectId());
		if (logBean == null) {
			if (belong instanceof Collection) {
				// 容器使用共享的日志。需要先去查询，没有的话才创建。
				logBean = (LogBean)Transaction.getCurrent().getLog(
						belong.parent().objectId() + belong.variableId());
			}
			if (logBean == null)
				logBean = belong.createLogBean();
			beans.put(belong.objectId(), logBean);
		}
		logBean.collect(this, belong, log);
	}

	public void collectRecord(Transaction.RecordAccessed ar) {
		var tkey = ar.tableKey();
		var r = records.get(tkey);
		if (r == null) {
			// put record only
			r = new Record();
			var table = ar.getOrigin().getTable();
			r.setTableTemplateId(table.getTemplateId());
			r.setTableTemplateName(table.getTemplateName());
			records.put(tkey, r);
		}

		r.collect(ar);
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) {
		if (holder.isLeaderRequest()) {
			if (Rocks.isDebugEnabled)
				Rocks.logger.debug("{} LeaderApply", rocks.getRaft().getName());
			transaction.leaderApply(this);
		} else {
			// Rocks.logger.debug("{} followerApply", rocks.getRaft().getName());
			rocks.followerApply(this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteUInt(records.size());
		for (var r : records.entrySet()) {
			// encode TableTemplate
			bb.WriteUInt(r.getValue().tableTemplateId);
			bb.WriteString(r.getValue().tableTemplateName);

			// encode TableKey
			r.getValue().setTableByName(this, r.getValue().tableTemplateName);
			bb.WriteString(r.getKey().name);
			((Table<Object, Bean>)r.getValue().table).encodeKey(bb, r.getKey().key);

			// encode record
			r.getValue().encode(bb);
		}
		bb.WriteUInt(atomicLongs.size());
		for (var it = atomicLongs.iterator(); it.moveToNext(); ) {
			bb.WriteUInt(it.key());
			bb.WriteLong(it.value());
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var tkey = new TableKey();
			var r = new Record();

			r.setTableTemplateId(bb.ReadUInt());
			r.setTableTemplateName(bb.ReadString());

			tkey.name = bb.ReadString();
			r.setTableByName(this, r.getTableTemplateName());
			tkey.key = r.table.decodeKey(bb);

			r.decode(bb);

			records.put(tkey, r);
		}

		for (int i = bb.ReadUInt(); i > 0; i--) {
			var index = bb.ReadUInt();
			var value = bb.ReadLong();
			atomicLongs.put(index, value);
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, records);
		return sb.toString();
	}
}
