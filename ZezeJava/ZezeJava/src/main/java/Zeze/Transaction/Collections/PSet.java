package Zeze.Transaction.Collections;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.Transaction;

import org.jspecify.annotations.Nullable;
import Zeze.Collections.Persistents;

/** 事务 Set 基类：基于 pcollections 持久化集合的 Set 视图，托管下的修改经 LogSet 记账。 */
public abstract class PSet<V> extends Collection implements Set<V> {
	org.pcollections.PSet<V> set = Persistents.set();

	@Override
	public abstract boolean add(V item);

	@Override
	public abstract boolean remove(Object item);

	@Override
	public abstract void clear();

	public final org.pcollections.PSet<V> getSet() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return set;
			//noinspection DataFlowIssue
			TransactionLog log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return set;
			@SuppressWarnings("unchecked")
			var setLog = (LogSet<V>)log;
			return setLog.getValue();
		}
		return set;
	}

	@Override
	public final int size() {
		return getSet().size();
	}

	@Override
	public final boolean isEmpty() {
		return getSet().isEmpty();
	}

	@Override
	public final boolean contains(Object v) {
		return getSet().contains(v);
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		return getSet().containsAll(c);
	}

	@Deprecated
	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public final void copyTo(V[] array, int arrayIndex) {
		for (V v : getSet())
			array[arrayIndex++] = v;
	}

	@Override
	public Object[] toArray() {
		return getSet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return getSet().toArray(a);
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = getSet().iterator();
			private @Nullable V next;

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public V next() {
				return next = it.next();
			}

			@Override
			public void remove() {
				if (next == null)
					throw new IllegalStateException("iterator remove() before next()");
				// 校验元素仍在当前集合中，避免外部移除后退化为静默 no-op。
				if (!getSet().contains(next))
					throw new ConcurrentModificationException("structural modification during iteration");
				PSet.this.remove(next);
				next = null;
			}
		};
	}

	@Override
	public int hashCode() {
		return getSet().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		// 按 Set 接口契约比较内容，确保与标准 Set 之间的对称性。
		return o instanceof java.util.Set && getSet().equals(o);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getSet());
		return sb.toString();
	}
}
