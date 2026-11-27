package Zeze.Transaction.GTable;


import java.util.ListIterator;
import java.util.NoSuchElementException;

/** 按下标访问定长序列的不可修改 {@link ListIterator} 骨架实现，不支持 {@link #remove}、{@link #set} 和 {@link #add}。 */
@SuppressWarnings("deprecation")
abstract class AbstractIndexedListIterator<E> extends UnmodifiableListIterator<E> {
	private final int size;
	private int position;

	/** 返回指定下标的元素，供 {@link #next()} 和 {@link #previous()} 调用。 */
	protected abstract E get(int index);

	/**
	 * 构造指定长度与初始位置的列表迭代器。
	 *
	 * @throws IndexOutOfBoundsException {@code position} 为负或大于 {@code size} 时抛出
	 * @throws IllegalArgumentException  {@code size} 为负时抛出
	 */
	protected AbstractIndexedListIterator(int size, int position) {
		Utils.checkPositionIndex(position, size);
		this.size = size;
		this.position = position;
	}

	@Override
	public final boolean hasNext() {
		return position < size;
	}

	@Override
	public final E next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return get(position++);
	}

	@Override
	public final int nextIndex() {
		return position;
	}

	@Override
	public final boolean hasPrevious() {
		return position > 0;
	}

	@Override
	public final E previous() {
		if (!hasPrevious()) {
			throw new NoSuchElementException();
		}
		return get(--position);
	}

	@Override
	public final int previousIndex() {
		return position - 1;
	}
}
