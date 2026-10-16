package Zeze.Transaction.Collections;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;

/** 事务 Set 日志基类：持有持久化集合的当前值，提交时整体写回。 */
public abstract class LogSet<V> extends LogBean {
	private org.pcollections.PSet<V> value;

	protected LogSet(@Nullable Bean belong, int varId, @Nullable Bean self, org.pcollections.PSet<V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

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

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PSet<V>)Objects.requireNonNull(getThis())).set = value;
	}
}
