package Zeze.Transaction.Collections;

/** Final persistent list state for commits without change consumers. */
final class LogListNoDelta<V> extends LogList<V> {

	LogListNoDelta(PList<V> source, org.pcollections.PVector<V> value) {
		super(source, value);
	}

	@Override
	public boolean hasDelta() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogListNoDelta<V> beginSavepoint() {
		return new LogListNoDelta<>((PList<V>)source, getValue());
	}
}
