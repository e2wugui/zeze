package Zeze.Raft.RocksRaft;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import org.pcollections.PVector;

public abstract class CollList<V> extends Collection implements Iterable<V> {
	public PVector<V> _list = org.pcollections.Empty.vector();

	public abstract boolean add(V item);

	public abstract boolean remove(V item);

	public abstract void clear();

	public abstract V set(int index, V item);

	public abstract void add(int index, V item);

	public abstract V remove(int index);

	protected final PVector<V> getList() {
		if (isManaged()) {
			if (Transaction.getCurrent() == null)
				return _list;
			Log log = Transaction.getCurrent().GetLog(getParent().getObjectId() + getVariableId());
			if (log == null)
				return _list;
			@SuppressWarnings("unchecked")
			var listLog = (LogList<V>)log;
			return listLog.getValue();
		}
		return _list;
	}

	public final boolean isEmpty() {
		return getList().isEmpty();
	}

	public final int size() {
		return getList().size();
	}

	public final boolean contains(V v) {
		return getList().contains(v);
	}

	public int indexOf(V o) {
		return getList().indexOf(o);
	}

	public int lastIndexOf(V o) {
		return getList().lastIndexOf(o);
	}

	public V get(int index) {
		return getList().get(index);
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = getList().iterator();
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
				CollList.this.remove(next);
			}
		};
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getList());
		return sb.toString();
	}
}
