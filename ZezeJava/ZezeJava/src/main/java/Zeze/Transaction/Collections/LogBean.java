package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.IntHashMap;

public class LogBean extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Collections.LogBean");

	private IntHashMap<Log> Variables;
	private Zeze.Transaction.Bean This;

	public LogBean() {
		super(TYPE_ID);
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	public LogBean(int typeId) {
		super(typeId);
	}

	public LogBean(String typeName) {
		super(typeName);
	}

	public final IntHashMap<Log> getVariables() {
		return Variables;
	}

	public final IntHashMap<Log> getVariablesOrNew() {
		var variables = Variables;
		if (variables == null)
			Variables = variables = new IntHashMap<>();
		return variables;
	}

	public final Bean getThis() {
		return This;
	}

	public final void setThis(Bean value) {
		This = value;
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
		var vars = Variables;
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
	public void decode(ByteBuffer bb) {
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
		} else if (Variables != null)
			Variables.clear();
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
		ByteBuffer.BuildSortedString(sb, Variables);
		return sb.toString();
	}
}
