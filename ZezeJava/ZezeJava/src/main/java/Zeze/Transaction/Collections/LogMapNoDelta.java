package Zeze.Transaction.Collections;

/** No consumers: retain persistent state without structural change collections. */
final class LogMapNoDelta<K, V> extends LogMap<K, V> {
	LogMapNoDelta(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source, value);
	}

	@Override
	public boolean hasDelta() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogMapNoDelta<K, V> beginSavepoint() {
		return new LogMapNoDelta<>((PMap<K, V>)source, getValue());
	}
}
