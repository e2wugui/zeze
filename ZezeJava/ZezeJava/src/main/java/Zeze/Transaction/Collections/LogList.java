package Zeze.Transaction.Collections;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import org.pcollections.PVector;

/** 事务 List 日志基类：持有持久化向量的当前值，提交时整体写回。 */
public abstract class LogList<V> extends LogBean {
	private PVector<V> value;

	protected LogList(@Nullable Bean belong, int varId, @Nullable Bean self, PVector<V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

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

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PList<V>)Objects.requireNonNull(getThis())).list = value;
	}
}
