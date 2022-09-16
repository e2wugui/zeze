package Zeze.Transaction.Collections;

import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.pcollections.PVector;

public abstract class LogList<V> extends LogBean {
	private PVector<V> value;

	public LogList(int typeId) {
		super(typeId);
	}

	public LogList(String typeName) {
		super(typeName);
	}

	final PVector<V> getValue() {
		return value;
	}

	final void setValue(PVector<V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Zeze.Transaction.Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PList<V>)getThis()).list = value;
	}
}
