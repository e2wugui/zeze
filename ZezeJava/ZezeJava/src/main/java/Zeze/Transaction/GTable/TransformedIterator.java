package Zeze.Transaction.GTable;

import java.util.Iterator;

/** 对底层迭代器元素按需转换的内部迭代器基类。 */
abstract class TransformedIterator<F, T> implements Iterator<T> {
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
