package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

/** Map1 transaction notes; no codec metadata is retained. */
public final class LogMap1Delta<K, V> extends LogMapDelta<K, V> {
	LogMap1Delta(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source, value);
	}

	@Override
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		return !java.util.Objects.equals(oldValue, newValue);
	}

	@Override
	public LogMap1Delta<K, V> beginSavepoint() {
		return new LogMap1Delta<>(source, getValue());
	}
}
