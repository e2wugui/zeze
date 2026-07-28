package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;

public abstract class LogSortedMap<K extends Comparable<K>, V> extends LogBean {
	private @NotNull org.pcollections.PSortedMap<K, V> value;

	protected LogSortedMap(Bean belong, int varId, Bean self, @NotNull org.pcollections.PSortedMap<K, V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

	public final @NotNull org.pcollections.PSortedMap<K, V> getValue() {
		return value;
	}

	final void setValue(@NotNull org.pcollections.PSortedMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PSortedMap<K, V>)getThis()).map = value;
	}
}
