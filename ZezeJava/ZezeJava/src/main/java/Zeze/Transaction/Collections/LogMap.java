package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;

public abstract class LogMap<K, V> extends LogBean {
	private @NotNull org.pcollections.PMap<K, V> value;

	protected LogMap(Bean belong, int varId, Bean self, @NotNull org.pcollections.PMap<K, V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

	public final @NotNull org.pcollections.PMap<K, V> getValue() {
		return value;
	}

	final void setValue(@NotNull org.pcollections.PMap<K, V> value) {
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
