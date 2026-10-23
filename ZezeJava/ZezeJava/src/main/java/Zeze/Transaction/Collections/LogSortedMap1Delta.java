package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

/** SortedMap1 transaction notes; no codec metadata is retained. */
public final class LogSortedMap1Delta<K extends Comparable<K>, V> extends LogSortedMapDelta<K, V> {
	public LogSortedMap1Delta(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		super(source, value);
	}

	@Override
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		return !java.util.Objects.equals(oldValue, newValue);
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogSortedMap1Delta<K, V> beginSavepoint() {
		return new LogSortedMap1Delta<>((PSortedMap<K, V>)source, getValue());
	}
}
