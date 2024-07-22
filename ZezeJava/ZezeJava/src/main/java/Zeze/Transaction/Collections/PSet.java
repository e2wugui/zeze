package Zeze.Transaction.Collections;

import java.util.Iterator;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

public abstract class PSet<V> extends Collection implements Set<V> {
	@NotNull org.pcollections.PSet<V> set = Empty.set();

	@Override
	public abstract boolean add(@NotNull V item);

	@Override
	public abstract boolean remove(@NotNull Object item);

	@Override
	public abstract void clear();

	public final @NotNull org.pcollections.PSet<V> getSet() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return set;
			//noinspection DataFlowIssue
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
	public final boolean contains(@NotNull Object v) {
		return getSet().contains(v);
	}

	@Override
	public boolean containsAll(@NotNull java.util.Collection<?> c) {
		return getSet().containsAll(c);
	}

	@Deprecated // unsupported
	@Override
	public boolean retainAll(@NotNull java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public final void copyTo(V @NotNull [] array, int arrayIndex) {
		for (V v : getSet())
			array[arrayIndex++] = v;
	}

	@Override
	public Object @NotNull [] toArray() {
		return getSet().toArray();
	}

	@Override
	public <T> T @NotNull [] toArray(T @NotNull [] a) {
		return getSet().toArray(a);
	}

	@Override
	public @NotNull Iterator<V> iterator() {
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
	public int hashCode() {
		return getSet().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof PSet && getSet().equals(((PSet<?>)o).getSet());
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getSet());
		return sb.toString();
	}
}
