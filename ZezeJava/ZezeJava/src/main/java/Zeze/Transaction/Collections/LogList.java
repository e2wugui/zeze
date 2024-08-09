package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;
import org.pcollections.PVector;

public abstract class LogList<V> extends LogBean {
	private @NotNull PVector<V> value;

	protected LogList(Bean belong, int varId, Bean self, @NotNull PVector<V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

	final @NotNull PVector<V> getValue() {
		return value;
	}

	final void setValue(@NotNull PVector<V> value) {
		this.value = value;
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PList<V>)getThis()).list = value;
	}
}
