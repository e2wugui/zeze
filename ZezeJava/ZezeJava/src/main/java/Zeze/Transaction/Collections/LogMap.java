package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;

public abstract class LogMap<K, V> extends LogBean {
	private org.pcollections.PMap<K, V> value;

	@Override
	public abstract int getTypeId();

	public final @NotNull org.pcollections.PMap<K, V> getValue() {
		return value;
	}

	public final void setValue(@NotNull org.pcollections.PMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PMap<K, V>)getThis()).map = value;
	}
}
