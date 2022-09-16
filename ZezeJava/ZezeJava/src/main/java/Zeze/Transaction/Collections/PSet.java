package Zeze.Transaction.Collections;

import java.util.Iterator;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;

public abstract class PSet<V> extends Collection implements Set<V> {
	public org.pcollections.PSet<V> set = org.pcollections.Empty.set();

	@Override
	public abstract boolean add(V item);

	@Override
	public abstract boolean remove(Object item);

	@Override
	public abstract void clear();

	protected final org.pcollections.PSet<V> getSet() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return set;
			Log log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return set;
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)log;
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

	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public final void copyTo(V[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : getSet()) {
			array[index++] = e;
		}
	}

	@Override
	public Object[] toArray() {
		return getSet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		//noinspection SuspiciousToArrayCall
		return getSet().toArray(a);
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = getSet().iterator();
			private V next;

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
				PSet.this.remove(next);
			}
		};
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getSet());
		return sb.toString();
	}
}
