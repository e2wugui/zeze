package Zeze.Transaction.Collections;

import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;

public abstract class LogSet<V> extends LogBean {
	private org.pcollections.PSet<V> value;

	public LogSet(int typeId) {
		super(typeId);
	}

	public LogSet(String typeName) {
		super(typeName);
	}

	public final org.pcollections.PSet<V> getValue() {
		return value;
	}

	public final void setValue(org.pcollections.PSet<V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Zeze.Transaction.Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PSet<V>)getThis()).set = value;
	}
}
