package Zeze.Raft.RocksRaft;

/**
 * Set 容器日志基类：在 LogBean 之上携带容器的当前值（PSet）。
 */
public abstract class LogSet<V> extends LogBean {
	private org.pcollections.PSet<V> value;

	public LogSet(int typeId) {
		super(typeId);
	}

	public final org.pcollections.PSet<V> getValue() {
		return value;
	}

	public final void setValue(org.pcollections.PSet<V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
