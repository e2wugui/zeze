package Zeze.Transaction.Collections;

/** Ordered structural notes for a value list. */
final class LogList1Delta<V> extends LogListDelta<V> {
	LogList1Delta(PList<V> source, org.pcollections.PVector<V> value) {
		super(source, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogList1Delta<V> beginSavepoint() {
		return new LogList1Delta<>((PList<V>)source, getValue());
	}
}
