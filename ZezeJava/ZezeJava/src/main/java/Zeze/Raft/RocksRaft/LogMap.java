package Zeze.Raft.RocksRaft;

public abstract class LogMap<K, V> extends LogBean {
	private org.pcollections.PMap<K, V> value;

	public LogMap(int typeId) {
		super(typeId);
	}

	public LogMap(String typeName) {
		super(typeName);
	}

	public final org.pcollections.PMap<K, V> getValue() {
		return value;
	}

	public final void setValue(org.pcollections.PMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
