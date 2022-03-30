package Zeze.Raft.RocksRaft;

public abstract class LogMap<K, V> extends LogBean {
	private org.pcollections.PMap<K, V> Value;

	public LogMap(int typeId) {
		super(typeId);
	}

	public LogMap(String typeName) {
		super(typeName);
	}

	public final org.pcollections.PMap<K, V> getValue() {
		return Value;
	}

	public final void setValue(org.pcollections.PMap<K, V> value) {
		Value = value;
	}

	@Override
	public void Collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
