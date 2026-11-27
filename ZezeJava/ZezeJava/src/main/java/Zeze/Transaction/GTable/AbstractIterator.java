package Zeze.Transaction.GTable;

import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

/** 基于惰性状态机的不可修改迭代器骨架，子类实现 {@link #computeNext()} 产出元素并在结束时调用 {@link #endOfData()}。 */
public abstract class AbstractIterator<T> extends UnmodifiableIterator<T> {
	private State state = State.NOT_READY;

	protected AbstractIterator() {}

	private enum State {
		/** 已计算出下一个元素但尚未返回。 */
		READY,

		/** 尚未计算下一个元素，或上一元素已被返回。 */
		NOT_READY,

		/** 迭代已结束。 */
		DONE,

		/** 计算下一元素时发生异常，迭代器已失效。 */
		FAILED,
	}

	@Nullable private T next;

	/**
	 * 计算并返回下一个元素；无剩余元素时必须调用 {@link #endOfData()}。
	 * <p>调用期间不可回调本实例的 {@link #hasNext()}、{@link #next()} 或 {@link #peek()}；
	 * 若抛出异常，后续访问该迭代器将抛出 {@link IllegalStateException}。
	 */
	@Nullable
	protected abstract T computeNext();

	/** 标记迭代结束并返回 {@code null}，便于 {@link #computeNext()} 直接 {@code return endOfData();}。 */
	@Nullable
	protected final T endOfData() {
		state = State.DONE;
		return null;
	}

	@Override
	public final boolean hasNext() {
		Utils.checkState(state != State.FAILED);
		return switch (state) {
			case DONE -> false;
			case READY -> true;
			default -> tryToComputeNext();
		};
	}

	private boolean tryToComputeNext() {
		state = State.FAILED; // 先置 FAILED，防止异常或重入后留下无效状态。
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
		// hasNext() 已保证 next 写入了当前元素。
		T result = Utils.uncheckedCastNullableTToT(next);
		next = null;
		return result;
	}

	/** 返回下一个元素但不推进迭代位置。 */
	public final T peek() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		// hasNext() 已保证 next 写入了当前元素。
		return Utils.uncheckedCastNullableTToT(next);
	}
}
