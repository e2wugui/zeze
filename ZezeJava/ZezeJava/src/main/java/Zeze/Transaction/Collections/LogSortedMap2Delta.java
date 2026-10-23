package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** SortedMap2 transaction notes; no codec metadata is retained. */
final class LogSortedMap2Delta<K extends Comparable<K>, V extends Bean> extends LogSortedMapDelta<K, V> {
	LogSortedMap2Delta(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		super(source, value);
	}

	@Override
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		return oldValue != newValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogSortedMap2Delta<K, V> beginSavepoint() {
		return new LogSortedMap2Delta<>((PSortedMap<K, V>)source, getValue());
	}
}
