package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Map2 transaction notes; no codec metadata is retained. */
final class LogMap2Delta<K, V extends Bean> extends LogMapDelta<K, V> {
	LogMap2Delta(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source, value);
	}

	@Override
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		return oldValue != newValue;
	}

	@Override
	public LogMap2Delta<K, V> beginSavepoint() {
		return new LogMap2Delta<>(source, getValue());
	}
}
