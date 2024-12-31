package Zeze.Transaction.GTable;

import java.util.Iterator;

/**
 * An iterator that transforms a backing iterator; for internal use. This avoids the object overhead
 * of constructing a {@link com.google.common.base.Function Function} for internal methods.
 *
 * @author Louis Wasserman
 */
abstract class TransformedIterator<F extends Object, T extends Object>
		implements Iterator<T> {
	final Iterator<? extends F> backingIterator;

	TransformedIterator(Iterator<? extends F> backingIterator) {
		this.backingIterator = Utils.checkNotNull(backingIterator);
	}

	abstract T transform(F from);

	@Override
	public final boolean hasNext() {
		return backingIterator.hasNext();
	}

	@Override
	public final T next() {
		return transform(backingIterator.next());
	}

	@Override
	public final void remove() {
		backingIterator.remove();
	}
}
