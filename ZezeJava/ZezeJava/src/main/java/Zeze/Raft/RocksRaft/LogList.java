package Zeze.Raft.RocksRaft;

import org.pcollections.PVector;

public abstract class LogList<V> extends LogBean {
	private PVector<V> Value;

	public LogList(int typeId) {
		super(typeId);
	}

	public LogList(String typeName) {
		super(typeName);
	}

	final PVector<V> getValue() {
		return Value;
	}

	final void setValue(PVector<V> value) {
		Value = value;
	}

	@Override
	public void Collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
