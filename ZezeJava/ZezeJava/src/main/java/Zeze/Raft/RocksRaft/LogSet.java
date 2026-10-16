package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

/**
 * Set 容器日志基类：在 LogBean 之上携带容器的当前值（PSet）。
 */
public abstract class LogSet<V> extends LogBean {
	private org.pcollections.@Nullable PSet<V> value;

	public LogSet(int typeId) {
		super(typeId);
	}

	public final org.pcollections.@Nullable PSet<V> getValue() {
		return value;
	}

	public final void setValue(org.pcollections.@Nullable PSet<V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
