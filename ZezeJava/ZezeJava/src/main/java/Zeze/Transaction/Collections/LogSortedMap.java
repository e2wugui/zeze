package Zeze.Transaction.Collections;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;

/** 事务 SortedMap 日志基类：持有持久化有序映射的当前值，提交时整体写回。 */
public abstract class LogSortedMap<K extends Comparable<K>, V> extends LogBean {
	private org.pcollections.PSortedMap<K, V> value;

	protected LogSortedMap(@Nullable Bean belong, int varId, @Nullable Bean self, org.pcollections.PSortedMap<K, V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

	public final org.pcollections.PSortedMap<K, V> getValue() {
		return value;
	}

	final void setValue(org.pcollections.PSortedMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		((PSortedMap<K, V>)Objects.requireNonNull(getThis())).map = value;
	}
}
