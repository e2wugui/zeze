package Zeze.Raft.RocksRaft;

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
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
