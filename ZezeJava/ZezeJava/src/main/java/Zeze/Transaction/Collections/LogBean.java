package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.IntHashMap;
import org.jetbrains.annotations.NotNull;

public class LogBean extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Collections.LogBean");

	private IntHashMap<Log> variables;
	private Bean self;

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	public final IntHashMap<Log> getVariables() {
		return variables;
	}

	public final @NotNull IntHashMap<Log> getVariablesOrNew() {
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

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	// LogBean仅在_final_commit的Collect过程中创建，不会参与Savepoint。
	@Override
	public @NotNull Log beginSavepoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var vars = variables;
		//System.out.println("LogBean.this=" + getThis().getClass().getName());
		if (vars != null) {
			bb.WriteUInt(vars.size());
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var log = it.value();
				bb.WriteInt4(log.getTypeId());
				bb.WriteUInt(log.getVariableId());
				log.encode(bb);
				//if (log.getTypeId() == -1120993624)
				//	System.out.println("key=" + it.key() + " typeId=" + log.getTypeId() + " varId=" + log.getVariableId() + " name=" + log.getClass().getName());
			}
		} else
			bb.WriteUInt(0);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
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
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		if (getVariablesOrNew().putIfAbsent(vlog.getVariableId(), vlog) == null)
			changes.collect(recent, this); // 向上传递
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildSortedString(sb, variables);
		return sb.toString();
	}
}
