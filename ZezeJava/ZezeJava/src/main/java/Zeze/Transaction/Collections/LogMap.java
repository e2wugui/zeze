package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;

/** 事务 Map 日志基类：持有持久化映射的当前值，提交时整体写回。 */
public abstract class LogMap<K, V> extends LogBean {
	private org.pcollections.PMap<K, V> value;

	protected LogMap(@Nullable Bean belong, int varId, @Nullable Bean self, org.pcollections.PMap<K, V> value) {
		super(belong, varId, self);
		this.value = value;
	}

	@Override
	public abstract int getTypeId();

	public final org.pcollections.PMap<K, V> getValue() {
		return value;
	}

	final void setValue(org.pcollections.PMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		//noinspection DataFlowIssue
		((PMap<K, V>)getThis()).map = value;
	}
}
