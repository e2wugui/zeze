package Zeze.Transaction.GTable;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An iterator that transforms a backing iterator; for internal use. This avoids the object overhead
 * of constructing a {@link com.google.common.base.Function Function} for internal methods.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
abstract class TransformedIterator<F extends @Nullable Object, T extends @Nullable Object>
		implements Iterator<T> {
	final Iterator<? extends F> backingIterator;

	TransformedIterator(Iterator<? extends F> backingIterator) {
		this.backingIterator = checkNotNull(backingIterator);
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
