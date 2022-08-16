package Zeze.Raft.RocksRaft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashMap;

public final class Changes extends Zeze.Raft.Log {
	private final Rocks rocks;
	private final LongHashMap<LogBean> Beans = new LongHashMap<>(); // 收集日志时,记录所有Bean修改. key is Bean.ObjectId
	private final HashMap<TableKey, Record> Records = new HashMap<>(); // 收集记录的修改,以后需要序列化传输.
	private final IntHashMap<Long> AtomicLongs = new IntHashMap<>();
	private Transaction transaction;

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
		return Beans;
	}

	public HashMap<TableKey, Record> getRecords() {
		return Records;
	}

	public IntHashMap<Long> getAtomicLongs() {
		return AtomicLongs;
	}

	public static final class Record {
		public static final int Remove = 0;
		public static final int Put = 1;
		public static final int Edit = 2;

		private int TableTemplateId;
		private String TableTemplateName;
		private int State;
		private Bean PutValue;
		private final Set<LogBean> LogBean = new HashSet<>();
		// 所有的日志修改树，key is Record.Value。不会被序列化。
		private final IdentityHashMap<Bean, LogBean> LogBeans = new IdentityHashMap<>();
		@SuppressWarnings("rawtypes")
		public Table Table;

		public void setTableTemplateId(int value) {
			TableTemplateId = value;
		}

		public String getTableTemplateName() {
			return TableTemplateName;
		}

		public void setTableTemplateName(String value) {
			TableTemplateName = value;
		}

		public int getState() {
			return State;
		}

		public Bean getPutValue() {
			return PutValue;
		}

		public Set<LogBean> getLogBean() {
			return LogBean;
		}

		public IdentityHashMap<Bean, LogBean> getLogBeans() {
			return LogBeans;
		}

		public void SetTableByName(Changes changes, String templateName) {
			var tableTpl = changes.rocks.GetTableTemplate(templateName);
			if (tableTpl == null) {
				var names = new StringBuilder();
				for (String name : changes.rocks.getTableTemplates().keySet())
					names.append(' ').append(name);
				throw new NullPointerException("unknown table template: " + templateName + ", available:" + names);
			}
			Table = tableTpl.OpenTable(TableTemplateId);
		}

		public void Collect(Transaction.RecordAccessed ar) {
			if (ar.getPutLog() != null) { // put or remove
				PutValue = ar.getPutLog().Value;
				State = PutValue == null ? Remove : Put;
				return;
			}

			State = Edit;
			var logBean = LogBeans.get(ar.getOrigin().getValue());
			if (logBean != null)
				LogBean.add(logBean); // edit
		}

		public void Encode(ByteBuffer bb) {
			bb.WriteInt(State);
			switch (State) {
			case Remove:
				break;
			case Put:
				PutValue.Encode(bb);
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
				PutValue = Table.NewValue();
				PutValue.Decode(bb);
				break;
			case Edit:
				bb.Decode(LogBean, LogBean::new);
				break;
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			sb.append("State=").append(State).append(" PutValue=").append(PutValue);
			sb.append("\nLog=");
			ByteBuffer.BuildSortedString(sb, LogBean);
			sb.append("\nAllLog=");
			ByteBuffer.BuildSortedString(sb, LogBeans.values());
			return sb.toString();
		}
	}

	public void Collect(Bean recent, Log log) {
		Bean belong = log.getBelong();
		if (belong == null) {
			// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
			var r = Records.get(recent.tableKey());
			if (r == null) {
				r = new Record();
				var table = recent.rootInfo().getRecord().getTable();
				r.setTableTemplateId(table.getTemplateId());
				r.setTableTemplateName(table.getTemplateName());
				Records.put(recent.tableKey(), r);
			}
			r.getLogBeans().put(recent, (LogBean)log);
			return; // root
		}

		var logBean = Beans.get(belong.objectId());
		if (logBean == null) {
			if (belong instanceof Collection) {
				// 容器使用共享的日志。需要先去查询，没有的话才创建。
				logBean = (LogBean)Transaction.getCurrent().GetLog(
						belong.parent().objectId() + belong.variableId());
			}
			if (logBean == null)
				logBean = belong.CreateLogBean();
			Beans.put(belong.objectId(), logBean);
		}
		logBean.Collect(this, belong, log);
	}

	public void CollectRecord(Transaction.RecordAccessed ar) {
		var tkey = ar.tableKey();
		var r = Records.get(tkey);
		if (r == null) {
			// put record only
			r = new Record();
			var table = ar.getOrigin().getTable();
			r.setTableTemplateId(table.getTemplateId());
			r.setTableTemplateName(table.getTemplateName());
			Records.put(tkey, r);
		}

		r.Collect(ar);
	}

	@Override
	public void Apply(RaftLog holder, StateMachine stateMachine) {
		if (holder.getLeaderFuture() != null) {
			if (Rocks.isDebugEnabled)
				Rocks.logger.debug("{} LeaderApply", rocks.getRaft().getName());
			transaction.LeaderApply(this);
		} else {
			// Rocks.logger.debug("{} FollowerApply", rocks.getRaft().getName());
			rocks.FollowerApply(this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteUInt(Records.size());
		for (var r : Records.entrySet()) {
			// encode TableTemplate
			bb.WriteUInt(r.getValue().TableTemplateId);
			bb.WriteString(r.getValue().TableTemplateName);

			// encode TableKey
			r.getValue().SetTableByName(this, r.getValue().TableTemplateName);
			bb.WriteString(r.getKey().Name);
			r.getValue().Table.EncodeKey(bb, r.getKey().Key);

			// encode record
			r.getValue().Encode(bb);
		}
		bb.WriteUInt(AtomicLongs.size());
		for (var it = AtomicLongs.iterator(); it.moveToNext(); ) {
			bb.WriteUInt(it.key());
			bb.WriteLong(it.value());
		}
	}

	@Override
	public void Decode(ByteBuffer bb) {
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var tkey = new TableKey();
			var r = new Record();

			r.setTableTemplateId(bb.ReadUInt());
			r.setTableTemplateName(bb.ReadString());

			tkey.Name = bb.ReadString();
			r.SetTableByName(this, r.getTableTemplateName());
			tkey.Key = r.Table.DecodeKey(bb);

			r.Decode(bb);

			Records.put(tkey, r);
		}

		for (int i = bb.ReadUInt(); i > 0; i--) {
			var index = bb.ReadUInt();
			var value = bb.ReadLong();
			AtomicLongs.put(index, value);
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, Records);
		return sb.toString();
	}
}
