package Zeze.Transaction.GTable;

import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;

public abstract class AbstractIterator<T> extends UnmodifiableIterator<T> {
	private State state = State.NOT_READY;

	/** Constructor for use by subclasses. */
	protected AbstractIterator() {}

	private enum State {
		/** We have computed the next element and haven't returned it yet. */
		READY,

		/** We haven't yet computed or have already returned the element. */
		NOT_READY,

		/** We have reached the end of the data and are finished. */
		DONE,

		/** We've suffered an exception and are kaput. */
		FAILED,
	}

	@CheckForNull private T next;

	/**
	 * Returns the next element. <b>Note:</b> the implementation must call {@link #endOfData()} when
	 * there are no elements left in the iteration. Failure to do so could result in an infinite loop.
	 *
	 * <p>The initial invocation of {@link #hasNext()} or {@link #next()} calls this method, as does
	 * the first invocation of {@code hasNext} or {@code next} following each successful call to
	 * {@code next}. Once the implementation either invokes {@code endOfData} or throws an exception,
	 * {@code computeNext} is guaranteed to never be called again.
	 *
	 * <p>If this method throws an exception, it will propagate outward to the {@code hasNext} or
	 * {@code next} invocation that invoked this method. Any further attempts to use the iterator will
	 * result in an {@link IllegalStateException}.
	 *
	 * <p>The implementation of this method may not invoke the {@code hasNext}, {@code next}, or
	 * {@link #peek()} methods on this instance; if it does, an {@code IllegalStateException} will
	 * result.
	 *
	 * @return the next element if there was one. If {@code endOfData} was called during execution,
	 *     the return value will be ignored.
	 * @throws RuntimeException if any unrecoverable error happens. This exception will propagate
	 *     outward to the {@code hasNext()}, {@code next()}, or {@code peek()} invocation that invoked
	 *     this method. Any further attempts to use the iterator will result in an {@link
	 *     IllegalStateException}.
	 */
	@CheckForNull
	protected abstract T computeNext();

	/**
	 * Implementations of {@link #computeNext} <b>must</b> invoke this method when there are no
	 * elements left in the iteration.
	 *
	 * @return {@code null}; a convenience so your {@code computeNext} implementation can use the
	 *     simple statement {@code return endOfData();}
	 */
	@CheckForNull
	protected final T endOfData() {
		state = State.DONE;
		return null;
	}

	@Override
	public final boolean hasNext() {
		Utils.checkState(state != State.FAILED);
		switch (state) {
		case DONE:
			return false;
		case READY:
			return true;
		default:
		}
		return tryToComputeNext();
	}

	private boolean tryToComputeNext() {
		state = State.FAILED; // temporary pessimism
		next = computeNext();
		if (state != State.DONE) {
			state = State.READY;
			return true;
		}
		return false;
	}

	@Override
	public final T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		state = State.NOT_READY;
		// Safe because hasNext() ensures that tryToComputeNext() has put a T into `next`.
		T result = Utils.uncheckedCastNullableTToT(next);
		next = null;
		return result;
	}

	/**
	 * Returns the next element in the iteration without advancing the iteration, according to the
	 *
	 * <p>Implementations of {@code AbstractIterator} that wish to expose this functionality should
	 */
	public final T peek() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		// Safe because hasNext() ensures that tryToComputeNext() has put a T into `next`.
		return Utils.uncheckedCastNullableTToT(next);
	}
}
