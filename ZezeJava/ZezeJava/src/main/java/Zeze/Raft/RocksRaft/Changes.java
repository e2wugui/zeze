package Zeze.Raft.RocksRaft;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

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

/**
 * 一个事务提交产生的变更集（Raft 日志载体）：按记录收集 put/remove/edit 日志树与原子计数，
 * 序列化后在 follower 侧重放应用。
 */
public final class Changes extends Zeze.Raft.Log {
	private final @Nullable Rocks rocks;
	private final LongHashMap<LogBean> beans = new LongHashMap<>(); // 收集日志时记录所有 Bean 修改，键为 Bean.objectId
	private final HashMap<TableKey, Record> records = new HashMap<>(); // 收集待序列化传输的记录修改
	private final IntHashMap<Long> atomicLongs = new IntHashMap<>();
	private @Nullable Transaction transaction;
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(Changes.class.getName());

	@Override
	public long typeId() {
		return TypeId_;
	}

	public Changes(@Nullable Rocks r) {
		super(null);
		rocks = r;
	}

	public Changes(@Nullable Rocks r, Transaction t, @Nullable IRaftRpc req) {
		super(req);
		rocks = r;
		transaction = t;
	}

	public @Nullable Rocks getRocks() {
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
		private @Nullable String tableTemplateName;
		private int state;
		private @Nullable Bean putValue;
		private final Set<LogBean> logBean = new HashSet<>();
		// 所有的日志修改树，键为 Record.Value，不参与序列化。
		private final IdentityHashMap<Bean, LogBean> logBeans = new IdentityHashMap<>();
		public @Nullable Table<?, ?> table;

		public void setTableTemplateId(int value) {
			tableTemplateId = value;
		}

		public @Nullable String getTableTemplateName() {
			return tableTemplateName;
		}

		public void setTableTemplateName(String value) {
			tableTemplateName = value;
		}

		public int getState() {
			return state;
		}

		public @Nullable Bean getPutValue() {
			return putValue;
		}

		public Set<LogBean> getLogBean() {
			return logBean;
		}

		public IdentityHashMap<Bean, LogBean> getLogBeans() {
			return logBeans;
		}

		public void setTableByName(Changes changes, String templateName) {
			//noinspection DataFlowIssue
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
			if (ar.getPutLog() != null) {
				putValue = ar.getPutLog().value;
				state = putValue == null ? Remove : Put;
				return;
			}

			state = Edit;
			var logBean = logBeans.get(ar.getOrigin().getValue());
			if (logBean != null)
				this.logBean.add(logBean);
		}

		public void encode(ByteBuffer bb) {
			bb.WriteUInt(state);
			switch (state) {
			case Remove:
				break;
			case Put:
				//noinspection DataFlowIssue
				putValue.encode(bb);
				break;
			case Edit:
				bb.encode(logBean);
				break;
			}
		}

		public void decode(IByteBuffer bb) {
			putValue = null;
			logBean.clear();
			logBeans.clear();
			state = bb.ReadUInt();
			switch (state) {
			case Remove:
				break;
			case Put:
				//noinspection DataFlowIssue
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
			// 记录可能存在多个修改日志树，收集阶段全部保留，提交时再筛选。
			var r = records.get(recent.tableKey());
			if (r == null) {
				r = new Record();
				//noinspection DataFlowIssue
				var table = Objects.requireNonNull(recent.rootInfo().record().getTable());
				r.setTableTemplateId(table.getTemplateId());
				r.setTableTemplateName(table.getTemplateName());
				records.put(Objects.requireNonNull(recent.tableKey()), r);
			}
			r.getLogBeans().put(recent, (LogBean)log);
			return;
		}

		var logBean = beans.get(belong.objectId());
		if (logBean == null) {
			if (belong instanceof Collection) {
				// 容器使用共享的日志。需要先去查询，没有的话才创建。
				//noinspection DataFlowIssue
				logBean = (LogBean)Objects.requireNonNull(Transaction.getCurrent()).getLog(
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
			r = new Record();
			var table = Objects.requireNonNull(ar.getOrigin().getTable());
			r.setTableTemplateId(table.getTemplateId());
			r.setTableTemplateName(table.getTemplateName());
			records.put(Objects.requireNonNull(tkey), r);
		}

		r.collect(ar);
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) {
		if (holder.isLeaderRequest()) {
			if (Rocks.isDebugEnabled)
				//noinspection DataFlowIssue
				Rocks.logger.debug("{} LeaderApply", rocks.getRaft().getName());
			//noinspection DataFlowIssue
			transaction.leaderApply(this, holder);
		} else {
			//noinspection DataFlowIssue
			rocks.followerApply(this, holder);
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		// 持久化基类字段（unique/createTime/rpcResult），确保 follower 重放时写入唯一请求终态存根。
		super.encode(bb);
		bb.WriteUInt(records.size());
		for (var r : records.entrySet()) {
			bb.WriteUInt(r.getValue().tableTemplateId);
			bb.WriteString(r.getValue().getTableTemplateName());

			r.getValue().setTableByName(this, Objects.requireNonNull(r.getValue().getTableTemplateName()));
			bb.WriteString(r.getKey().name);
			//noinspection unchecked,DataFlowIssue
			((Table<Object, Bean>)r.getValue().table).encodeKey(bb, Objects.requireNonNull(r.getKey().key));

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
		super.decode(bb);
		// 解码完整的下一条事务日志，不能合并上一条或保留本地收集上下文。
		beans.clear();
		records.clear();
		atomicLongs.clear();
		transaction = null;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var tkey = new TableKey();
			var r = new Record();

			r.setTableTemplateId(bb.ReadUInt());
			r.setTableTemplateName(bb.ReadString());

			tkey.name = bb.ReadString();
			r.setTableByName(this, r.getTableTemplateName());
			//noinspection DataFlowIssue
			tkey.key = r.table.decodeKey(bb);

			r.decode(bb);

			records.put(Objects.requireNonNull(tkey), r);
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
