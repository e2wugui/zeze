package Zeze.Transaction.Collections;

/** No consumers: retain persistent state without structural change collections. */
final class LogSortedMapNoDelta<K extends Comparable<K>, V> extends LogSortedMap<K, V> {
	LogSortedMapNoDelta(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		super(source, value);
	}

	@Override
	public boolean hasDelta() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogSortedMapNoDelta<K, V> beginSavepoint() {
		return new LogSortedMapNoDelta<>((PSortedMap<K, V>)source, getValue());
	}
}
