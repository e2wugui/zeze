package Zeze.Raft.RocksRaft;

public abstract class LogSet<V> extends LogBean {
	private org.pcollections.PSet<V> Value;

	public LogSet(int typeId) {
		super(typeId);
	}

	public LogSet(String typeName) {
		super(typeName);
	}

	public final org.pcollections.PSet<V> getValue() {
		return Value;
	}

	public final void setValue(org.pcollections.PSet<V> value) {
		Value = value;
	}

	@Override
	public void Collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
