package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;

/** 事务 Set 日志基类：持有持久化集合的当前值，提交时整体写回。 */
public abstract class LogSet<V> extends LogBean {
	private @NotNull org.pcollections.PSet<V> value;

	protected LogSet(Bean belong, int varId, Bean self, @NotNull org.pcollections.PSet<V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

	public final @NotNull org.pcollections.PSet<V> getValue() {
		return value;
	}

	public final void setValue(@NotNull org.pcollections.PSet<V> value) {
		this.value = value;
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PSet<V>)getThis()).set = value;
	}
}
