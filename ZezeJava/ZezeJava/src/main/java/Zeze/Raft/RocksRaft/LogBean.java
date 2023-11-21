package Zeze.Raft.RocksRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.IntHashMap;

public class LogBean extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogBean");

	private IntHashMap<Log> variables;
	private Bean self;

	public LogBean() {
		super(TYPE_ID);
	}

	public LogBean(int typeId) {
		super(typeId);
	}

	public final IntHashMap<Log> getVariables() {
		return variables;
	}

	public final IntHashMap<Log> getVariablesOrNew() {
		var variables = this.variables;
		if (variables == null)
			this.variables = variables = new IntHashMap<>();
		return variables;
	}

	public final Bean getThis() {
		return self;
	}

	public final void setThis(Bean value) {
		self = value;
	}

	// LogBean仅在_final_commit的Collect过程中创建，不会参与Savepoint。
	@Override
	public Log beginSavepoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(ByteBuffer bb) {
		var vars = variables;
		if (vars != null) {
			bb.WriteUInt(vars.size());
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var log = it.value();
				bb.WriteInt4(log.getTypeId());
				bb.WriteUInt(log.getVariableId());
				log.encode(bb);
			}
		} else
			bb.WriteUInt(0);
	}

	@Override
	public void decode(IByteBuffer bb) {
		int n = bb.ReadUInt();
		if (n > 0) {
			var variables = getVariablesOrNew();
			variables.clear();
			for (; n > 0; --n) {
				var typeId = bb.ReadInt4();
				var log = create(typeId);

				var varId = bb.ReadUInt();
				log.setVariableId(varId);
				log.decode(bb);

				variables.put(varId, log);
			}
		} else if (variables != null)
			variables.clear();
	}

	// 仅发生在事务执行期间。decode-Apply不会执行到这里。
	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (getVariablesOrNew().put(vlog.getVariableId(), vlog) == null)
			changes.collect(recent, this); // 向上传递
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildSortedString(sb, variables);
		return sb.toString();
	}
}
