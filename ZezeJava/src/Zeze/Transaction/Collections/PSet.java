package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;
import java.util.*;

//C# TO JAVA CONVERTER TODO TASK: The interface type was changed to the closest equivalent Java type, but the methods implemented will need adjustment:
public abstract class PSet<E> extends PCollection implements Set<E>, IReadOnlySet<E> {
	private final tangible.Func1Param<ImmutableHashSet<E>, Log> _logFactory;

	protected ImmutableHashSet<E> set;

	protected PSet(long logKey, tangible.Func1Param<ImmutableHashSet<E>, Log> logFactory) {
		super(logKey);
		this._logFactory = ::logFactory;
		set = ImmutableHashSet<E>.Empty;
	}

	public final Log NewLog(ImmutableHashSet<E> value) {
		return _logFactory.invoke(value);
	}

	public abstract static class LogV extends Log {
		public ImmutableHashSet<E> Value;

		protected LogV(Bean bean, ImmutableHashSet<E> value) {
			super(bean);
			Value = value;
		}

		protected final void Commit(PSet<E> variable) {
			variable.set = Value;
		}
	}

	protected final ImmutableHashSet<E> getData() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
	 void setData(ImmutableHashSet<E> value)
			}
txn.VerifyRecordAccessed(this, true);
boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
return tempVar ? log.Value : set;
		}
else {return set;}
	}

	@Override
	public String toString() {
		return String.format("PSet%1$s", getData());
	}

	public final int size() {
		return getData().Count;
	}
	public final boolean isReadOnly() {
		return false;
	}

	public final void AddAll(java.lang.Iterable<E> items) {
		for (var item : items) {
			Add(item);
		}
	}

	public abstract boolean Add(E item);
	public abstract void Clear();
	public abstract void ExceptWith(java.lang.Iterable<E> other);
	public abstract void IntersectWith(java.lang.Iterable<E> other);
	public abstract boolean Remove(E item);
	public abstract void SymmetricExceptWith(java.lang.Iterable<E> other);
	public abstract void UnionWith(java.lang.Iterable<E> other);

	public final void Add(E item) {
		this.Add(item);
	}

	public final boolean contains(Object objectValue) {
		E item = (E)objectValue;
		return getData().Contains(item);
	}

	public final void CopyTo(E[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : getData()) {
			array[index++] = e;
		}
	}

	public final boolean IsProperSubsetOf(java.lang.Iterable<E> other) {
		return getData().IsProperSubsetOf(other);
	}

	public final boolean IsProperSupersetOf(java.lang.Iterable<E> other) {
		return getData().IsProperSupersetOf(other);
	}

	public final boolean IsSubsetOf(java.lang.Iterable<E> other) {
		return getData().IsSubsetOf(other);
	}

	public final boolean IsSupersetOf(java.lang.Iterable<E> other) {
		return getData().IsSupersetOf(other);
	}

	public final boolean SetEquals(java.lang.Iterable<E> other) {
		return getData().SetEquals(other);
	}

	public final boolean Overlaps(java.lang.Iterable<E> other) {
		return getData().Overlaps(other);
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

	public final ImmutableHashSet<E>.Enumerator GetEnumerator() {
		return getData().iterator();
	}
}