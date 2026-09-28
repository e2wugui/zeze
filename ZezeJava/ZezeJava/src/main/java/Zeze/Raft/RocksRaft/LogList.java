package Zeze.Raft.RocksRaft;

import org.pcollections.PVector;

/**
 * List 容器日志基类：在 LogBean 之上携带容器的当前值（PVector）。
 */
public abstract class LogList<V> extends LogBean {
	private PVector<V> value;

	public LogList(int typeId) {
		super(typeId);
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
