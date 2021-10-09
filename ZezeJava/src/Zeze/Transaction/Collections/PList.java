package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;
import java.util.*;

//C# TO JAVA CONVERTER TODO TASK: The interface type was changed to the closest equivalent Java type, but the methods implemented will need adjustment:
public abstract class PList<E> extends PCollection implements List<E>, IReadOnlyList<E> {
	private final tangible.Func1Param<ImmutableList<E>, Log> _logFactory;

	protected ImmutableList<E> list;

	protected PList(long logKey, tangible.Func1Param<ImmutableList<E>, Log> logFactory) {
		super(logKey);
		this._logFactory = ::logFactory;
		list = ImmutableList<E>.Empty;
	}

	public final Log NewLog(ImmutableList<E> value) {
		return _logFactory.invoke(value);
	}

	public abstract static class LogV extends Log {
		public ImmutableList<E> Value;

		protected LogV(Bean bean, ImmutableList<E> last) {
			super(bean);
			this.Value = last;
		}

		protected final void Commit(PList<E> variable) {
			variable.list = Value;
		}
	}

	protected final ImmutableList<E> getData() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return list;
			}
			txn.VerifyRecordAccessed(this, true);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			return tempVar ? log.Value : list;
		}
		return list;
	}
	public final int size() {
		return getData().Count;
	}

	@Override
	public String toString() {
		return String.format("PList%1$s", getData());
	}

	public abstract E get(int index);
	public abstract void set(int index, E value);

	public final boolean isReadOnly() {
		return false;
	}

	public abstract void Add(E item);
	public abstract void AddRange(java.lang.Iterable<E> items);
	public abstract void Clear();
	public abstract void Insert(int index, E item);
	public abstract boolean Remove(E item);
	public abstract void RemoveAt(int index);
	public abstract void RemoveRange(int index, int count);

	public final boolean contains(Object objectValue) {
		E item = (E)objectValue;
		return getData().Contains(item);
	}

	public final void CopyTo(E[] array, int arrayIndex) {
		getData().CopyTo(array, arrayIndex);
	}


	public final Iterator GetEnumerator() {
		if (this instanceof IEnumerable)
			return IEnumerable_GetEnumerator();
		else if (this instanceof IEnumerable)
			return IEnumerable_GetEnumerator();
		else
			throw new UnsupportedOperationException("No interface found.");
	}

	private Iterator IEnumerable_GetEnumerator() {
		return getData().iterator();
	}

	private Iterator<E> IEnumerable_GetEnumerator() {
		return getData().iterator();
	}

	public final ImmutableList<E>.Enumerator GetEnumerator() {
		return getData().iterator();
	}

	public final int indexOf(Object objectValue) {
		E item = (E)objectValue;
		return getData().IndexOf(item);
	}
}