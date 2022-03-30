package Zeze.Raft.RocksRaft;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;

public abstract class CollSet<V> extends Collection implements Iterable<V> {
	public org.pcollections.PSet<V> _set = org.pcollections.Empty.set();

	public abstract boolean add(V item);

	public abstract boolean remove(V item);

	public abstract void clear();

	protected final org.pcollections.PSet<V> getSet() {
		if (isManaged()) {
			if (Transaction.getCurrent() == null)
				return _set;
			Log log = Transaction.getCurrent().GetLog(getParent().getObjectId() + getVariableId());
			if (log == null)
				return _set;
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)log;
			return setLog.getValue();
		}
		return _set;
	}

	public final int size() {
		return getSet().size();
	}

	public final boolean Contains(V v) {
		return getSet().contains(v);
	}

	@Override
	public Iterator<V> iterator() {
		return getSet().iterator();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getSet());
		return sb.toString();
	}
}
