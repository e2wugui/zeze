/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Java adaptation of the list implementation in kotlinx.collections.immutable 0.5.2:
 * SmallPersistentVector, PersistentVector, PersistentVectorBuilder and Utils.
 * Small and trie vectors share one representation. Small persistent updates use
 * direct array copies; trie replacements copy only their path, while other
 * trie updates reuse Builder's copy-on-write algorithms.
 */
package Zeze.Transaction.Collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;

/**
 * Immutable vector with 32-element leaves and a nonempty tail (unless empty).
 * Updates share unchanged trie nodes. Builders own mutable nodes through a token
 * in slot 32; build() freezes shared trie nodes by replacing the token, or copies
 * the tail for small lists, so later writes cannot change snapshots.
 * Elements themselves are not copied. Builders are not thread safe.
 *
 * <p>API inventory for PList, PList1/PList2 and LogList/LogList1/LogList2:
 * <ul>
 * <li>Factory: persistentListOf() becomes empty().</li>
 * <li>Persistent operations (7): adding, addingAt, addingAll, replacingAt,
 * removing, removingAt, builder.</li>
 * <li>Builder operations (8): build, add(E), add(int,E), addAll(Collection),
 * set, remove(int), removeAll(Collection), clear; iterator::remove is also used.</li>
 * <li>Shared List reads (12): size, isEmpty, get, contains, containsAll,
 * indexOf, lastIndexOf, iterator, toArray(), toArray(T[]), equals, hashCode.
 * Standard collection reads are inherited from the JDK.</li>
 * </ul>
 * Bulk operations copy runs within leaves and share complete immutable leaves.
 * Filtering creates a snapshot only after the first match and publishes its
 * result after callbacks finish. Iterators cache the current leaf.
 * Bidirectional iteration is inherited from AbstractList without a custom implementation.
 */
public final class PersistentList<E> extends AbstractList<E> implements RandomAccess {
	private static final int BITS = 5;
	private static final int WIDTH = 1 << BITS;
	private static final int MASK = WIDTH - 1;
	private static final Object[] EMPTY_BUFFER = new Object[0];
	private static final PersistentList<?> EMPTY = new PersistentList<>(null, EMPTY_BUFFER, 0, 0);

	private final Object[] root;
	private final Object[] tail;
	private final int size;
	private final int rootShift;

	private PersistentList(Object[] root, Object[] tail, int size, int rootShift) {
		this.root = root;
		this.tail = tail;
		this.size = size;
		this.rootShift = rootShift;
	}

	@SuppressWarnings("unchecked")
	public static <E> PersistentList<E> empty() {
		return (PersistentList<E>)EMPTY;
	}

	private static int segment(int index, int shift) {
		return (index >>> shift) & MASK;
	}

	private static int rootSize(int size) {
		return size <= WIDTH ? 0 : (size - 1) & ~MASK;
	}

	private static void checkPosition(int index, int size) {
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException("index = " + index + ", size = " + size);
	}

	private static Object[] bufferFor(Object[] root, Object[] tail, int size, int shift, int index) {
		if (index >= rootSize(size))
			return tail;
		var buffer = root;
		for (; shift > 0; shift -= BITS)
			buffer = (Object[])buffer[segment(index, shift)];
		return buffer;
	}

	@Override
	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E get(int index) {
		Objects.checkIndex(index, size);
		return (E)bufferFor(root, tail, size, rootShift, index)[index & MASK];
	}

	public Builder<E> builder() {
		return new Builder<>(this);
	}

	public PersistentList<E> adding(E element) {
		if (size <= WIDTH) {
			if (size == WIDTH)
				return new PersistentList<>(tail, new Object[]{element}, size + 1, 0);
			var buffer = Arrays.copyOf(tail, size + 1);
			buffer[size] = element;
			return new PersistentList<>(null, buffer, size + 1, 0);
		}
		var builder = builder();
		builder.add(element);
		return builder.build();
	}

	public PersistentList<E> addingAt(int index, E element) {
		checkPosition(index, size);
		if (index == size)
			return adding(element);
		if (size <= WIDTH) {
			var buffer = Arrays.copyOf(tail, Math.min(size + 1, WIDTH));
			System.arraycopy(tail, index, buffer, index + 1, Math.min(size, MASK) - index);
			buffer[index] = element;
			return size == WIDTH
					? new PersistentList<>(buffer, new Object[]{tail[MASK]}, size + 1, 0)
					: new PersistentList<>(null, buffer, size + 1, 0);
		}
		var builder = builder();
		builder.add(index, element);
		return builder.build();
	}

	public PersistentList<E> addingAll(Collection<? extends E> elements) {
		var builder = builder();
		builder.addAll(elements);
		return builder.build();
	}

	public PersistentList<E> replacingAt(int index, E element) {
		Objects.checkIndex(index, size);
		if (size <= WIDTH) {
			if (tail[index] == element)
				return this;
			var buffer = tail.clone();
			buffer[index] = element;
			return new PersistentList<>(null, buffer, size, 0);
		}
		if (index >= rootSize(size)) {
			int tailIndex = index & MASK;
			if (tail[tailIndex] == element)
				return this;
			var buffer = Arrays.copyOf(tail, WIDTH);
			buffer[tailIndex] = element;
			return new PersistentList<>(root, buffer, size, rootShift);
		}
		var newRoot = replacingInRoot(root, rootShift, index, element);
		return newRoot == root ? this : new PersistentList<>(newRoot, tail, size, rootShift);
	}

	private static Object[] replacingInRoot(Object[] root, int shift, int index, Object element) {
		int bufferIndex = segment(index, shift);
		Object replacement = shift == 0 ? element
				: replacingInRoot((Object[])root[bufferIndex], shift - BITS, index, element);
		if (root[bufferIndex] == replacement)
			return root;
		var result = Arrays.copyOf(root, WIDTH);
		result[bufferIndex] = replacement;
		return result;
	}

	public PersistentList<E> removingAt(int index) {
		Objects.checkIndex(index, size);
		if (size <= WIDTH) {
			if (size == 1)
				return empty();
			var buffer = Arrays.copyOf(tail, size - 1);
			System.arraycopy(tail, index + 1, buffer, index, size - index - 1);
			return new PersistentList<>(null, buffer, size - 1, 0);
		}
		var builder = builder();
		builder.remove(index);
		return builder.build();
	}

	public PersistentList<E> removing(Object element) {
		int index = indexOf(element);
		return index >= 0 ? removingAt(index) : this;
	}

	@Override
	public Iterator<E> iterator() {
		return size <= WIDTH ? new BufferIterator<>(tail) : new ImmutableIterator();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return Collections.unmodifiableList(super.subList(fromIndex, toIndex));
	}

	private static final class BufferIterator<E> implements Iterator<E> {
		private final Object[] buffer;
		private int cursor;

		BufferIterator(Object[] buffer) {
			this.buffer = buffer;
		}

		@Override
		public boolean hasNext() {
			return cursor < buffer.length;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return (E)buffer[cursor++];
		}
	}

	private final class ImmutableIterator implements Iterator<E> {
		private int cursor;
		private Object[] leaf;

		@Override
		public boolean hasNext() {
			return cursor < size;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			int i = cursor & MASK;
			if (i == 0)
				leaf = bufferFor(root, tail, size, rootShift, cursor);
			cursor++;
			return (E)leaf[i];
		}
	}

	private static final class Ref {
		Object value;

		Ref(Object value) {
			this.value = value;
		}
	}

	public static final class Builder<E> extends AbstractList<E> implements RandomAccess {
		private PersistentList<E> built;
		private Object ownership = new Object();
		private Object[] root;
		private Object[] tail;
		private int size;
		private int rootShift;
		private int bufferModCount; // Copying a leaf on set is not a structural modification (upstream PR #339).

		private Builder(PersistentList<E> vector) {
			built = vector;
			root = vector.root;
			tail = vector.tail;
			size = vector.size;
			rootShift = vector.rootShift;
		}

		public PersistentList<E> build() {
			if (built == null) { // Small snapshots copy the tail, so the builder can keep owning its buffer.
				if (root != null)
					ownership = new Object();
				built = size == 0 ? empty()
						: new PersistentList<>(root, root == null ? Arrays.copyOf(tail, size) : tail, size, rootShift);
			}
			return built;
		}

		private boolean isMutable(Object[] buffer) {
			return buffer.length == WIDTH + 1 && buffer[WIDTH] == ownership;
		}

		private Object[] mutableBuffer() {
			var buffer = new Object[WIDTH + 1];
			buffer[WIDTH] = ownership;
			return buffer;
		}

		private Object[] mutableBufferWith(Object element) {
			var buffer = mutableBuffer();
			buffer[0] = element;
			return buffer;
		}

		private Object[] makeMutable(Object[] buffer) {
			if (buffer != null && isMutable(buffer))
				return buffer;
			var result = mutableBuffer();
			if (buffer != null)
				System.arraycopy(buffer, 0, result, 0, Math.min(buffer.length, WIDTH));
			return result;
		}

		@Override
		public int size() {
			return size;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E get(int index) {
			Objects.checkIndex(index, size);
			return (E)bufferFor(root, tail, size, rootShift, index)[index & MASK];
		}

		@Override
		public boolean add(E element) {
			built = null;
			modCount++;
			int tailSize = size - rootSize(size);
			if (tailSize < WIDTH) {
				tail = makeMutable(tail);
				tail[tailSize] = element;
				size++;
			} else
				pushFilledTail(root, tail, mutableBufferWith(element));
			return true;
		}

		private void pushFilledTail(Object[] root, Object[] filledTail, Object[] newTail) {
			if ((size >>> BITS) > (1 << rootShift)) {
				this.root = pushTail(mutableBufferWith(root), filledTail, rootShift + BITS);
				rootShift += BITS;
			} else
				this.root = root == null ? filledTail : pushTail(root, filledTail, rootShift);
			tail = newTail;
			size++;
		}

		private Object[] pushTail(Object[] root, Object[] tail, int shift) {
			int index = segment(size - 1, shift);
			var mutableRoot = makeMutable(root);
			mutableRoot[index] = shift == BITS ? tail
					: pushTail((Object[])mutableRoot[index], tail, shift - BITS);
			return mutableRoot;
		}

		@Override
		public boolean addAll(Collection<? extends E> elements) {
			var vector = snapshotVector(elements);
			var values = vector == null ? elements.toArray() : null;
			int count = vector != null ? vector.size : values.length;
			if (count == 0)
				return false;
			built = null;
			modCount++;
			if (vector != null)
				appendRange(vector, 0, count);
			else
				appendBuffer(values, 0, count, false);
			return true;
		}

		private static PersistentList<?> snapshotVector(Collection<?> elements) {
			if (elements instanceof PersistentList<?> vector)
				return vector;
			return elements instanceof Builder<?> builder ? builder.build() : null;
		}

		// These append helpers operate on private construction state, without modCount updates.
		@SuppressWarnings("unchecked")
		private void appendRange(PersistentList<?> source, int from, int to) {
			if (from == to)
				return;
			if (size == 0 && from == 0 && to == source.size) {
				root = source.root;
				tail = source.tail;
				size = source.size;
				rootShift = source.rootShift;
				built = (PersistentList<E>)source;
				return;
			}
			while (from < to) {
				var buffer = bufferFor(source.root, source.tail, source.size, source.rootShift, from);
				int offset = from & MASK;
				int count = Math.min(WIDTH - offset, to - from);
				appendBuffer(buffer, offset, count, offset == 0 && count == WIDTH);
				from += count;
			}
		}

		private void appendBuffer(Object[] values, int offset, int count, boolean shareLeaf) {
			if (count == 0)
				return;
			built = null;
			while (count > 0) {
				int tailSize = size - rootSize(size);
				if (shareLeaf && tailSize == 0) {
					tail = values;
					size += WIDTH;
					return;
				}
				if (tailSize == WIDTH) {
					if (shareLeaf) {
						pushFilledTail(root, tail, values);
						size += MASK;
						return;
					}
					pushFilledTail(root, tail, mutableBufferWith(values[offset++]));
					count--;
					continue;
				}
				int copied = Math.min(WIDTH - tailSize, count);
				tail = makeMutable(tail);
				System.arraycopy(values, offset, tail, tailSize, copied);
				size += copied;
				offset += copied;
				count -= copied;
				shareLeaf = false; // After a partial copy, only a suffix of the source leaf remains.
			}
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> elements) {
			checkPosition(index, size);
			if (index == size)
				return addAll(elements);
			var vector = snapshotVector(elements);
			var values = vector == null ? elements.toArray() : null;
			int count = vector != null ? vector.size : values.length;
			if (count == 0)
				return false;
			var source = build();
			var result = source.builder();
			result.truncate(index);
			if (vector != null)
				result.appendRange(vector, 0, count);
			else
				result.appendBuffer(values, 0, count, false);
			result.appendRange(source, index, source.size);
			adopt(result);
			return true;
		}

		@Override
		public void add(int index, E element) {
			checkPosition(index, size);
			if (index == size) {
				add(element);
				return;
			}
			built = null;
			modCount++;
			int rootSize = rootSize(size);
			if (index >= rootSize)
				insertIntoTail(root, index - rootSize, element);
			else {
				var carry = new Ref(null);
				var newRoot = insertIntoRoot(root, rootShift, index, element, carry);
				insertIntoTail(newRoot, 0, carry.value);
			}
		}

		private void insertIntoTail(Object[] root, int index, Object element) {
			int tailSize = size - rootSize(size);
			var mutableTail = makeMutable(tail);
			if (tailSize < WIDTH) {
				System.arraycopy(tail, index, mutableTail, index + 1, tailSize - index);
				mutableTail[index] = element;
				this.root = root;
				tail = mutableTail;
				size++;
			} else {
				Object last = tail[MASK];
				System.arraycopy(tail, index, mutableTail, index + 1, MASK - index);
				mutableTail[index] = element;
				pushFilledTail(root, mutableTail, mutableBufferWith(last));
			}
		}

		private Object[] insertIntoRoot(Object[] root, int shift, int index, Object element, Ref carry) {
			int bufferIndex = segment(index, shift);
			var mutableRoot = makeMutable(root);
			if (shift == 0) {
				carry.value = root[MASK];
				System.arraycopy(root, bufferIndex, mutableRoot, bufferIndex + 1, MASK - bufferIndex);
				mutableRoot[bufferIndex] = element;
			} else {
				mutableRoot[bufferIndex] = insertIntoRoot((Object[])mutableRoot[bufferIndex],
						shift - BITS, index, element, carry);
				for (int i = bufferIndex + 1; i < WIDTH && mutableRoot[i] != null; i++)
					mutableRoot[i] = insertIntoRoot((Object[])mutableRoot[i], shift - BITS, 0, carry.value, carry);
			}
			return mutableRoot;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E remove(int index) {
			Objects.checkIndex(index, size);
			built = null;
			modCount++;
			int rootSize = rootSize(size);
			if (index >= rootSize)
				return (E)removeFromTailAt(root, rootSize, rootShift, index - rootSize);
			var carry = new Ref(tail[0]);
			var newRoot = removeFromRootAt(root, rootShift, index, carry);
			removeFromTailAt(newRoot, rootSize, rootShift, 0);
			return (E)carry.value;
		}

		private Object removeFromTailAt(Object[] root, int rootSize, int shift, int index) {
			int tailSize = size - rootSize;
			Object removed = tail[index];
			if (tailSize == 1)
				pullLastBufferFromRoot(root, rootSize, shift);
			else {
				var mutableTail = makeMutable(tail);
				System.arraycopy(tail, index + 1, mutableTail, index, tailSize - index - 1);
				mutableTail[tailSize - 1] = null;
				this.root = root;
				tail = mutableTail;
				size--;
				rootShift = shift;
			}
			return removed;
		}

		private Object[] removeFromRootAt(Object[] root, int shift, int index, Ref carry) {
			int bufferIndex = segment(index, shift);
			var mutableRoot = makeMutable(root);
			if (shift == 0) {
				Object removed = root[bufferIndex];
				System.arraycopy(root, bufferIndex + 1, mutableRoot, bufferIndex, MASK - bufferIndex);
				mutableRoot[MASK] = carry.value;
				carry.value = removed;
			} else {
				int lastIndex = root[MASK] == null ? segment(rootSize(size) - 1, shift) : MASK;
				for (int i = lastIndex; i > bufferIndex; i--)
					mutableRoot[i] = removeFromRootAt((Object[])mutableRoot[i], shift - BITS, 0, carry);
				mutableRoot[bufferIndex] = removeFromRootAt((Object[])mutableRoot[bufferIndex],
						shift - BITS, index, carry);
			}
			return mutableRoot;
		}

		private void pullLastBufferFromRoot(Object[] root, int rootSize, int shift) {
			if (shift == 0) {
				this.root = null;
				tail = root != null ? root : EMPTY_BUFFER;
				size = rootSize;
				rootShift = 0;
				return;
			}
			var carry = new Ref(null);
			var newRoot = pullLastBuffer(root, shift, rootSize, carry);
			tail = (Object[])carry.value;
			size = rootSize;
			//noinspection DataFlowIssue
			if (newRoot[1] == null) {
				this.root = (Object[])newRoot[0];
				rootShift = shift - BITS;
			} else {
				this.root = newRoot;
				rootShift = shift;
			}
		}

		private Object[] pullLastBuffer(Object[] root, int shift, int rootSize, Ref carry) {
			int index = segment(rootSize - 1, shift);
			Object[] child;
			if (shift == BITS) {
				carry.value = root[index];
				child = null;
			} else
				child = pullLastBuffer((Object[])root[index], shift - BITS, rootSize, carry);
			if (child == null && index == 0)
				return null;
			var mutableRoot = makeMutable(root);
			mutableRoot[index] = child;
			return mutableRoot;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E set(int index, E element) {
			Objects.checkIndex(index, size);
			if (index >= rootSize(size)) {
				int tailIndex = index & MASK;
				E old = (E)tail[tailIndex];
				if (old == element) // Identity matters: equal but distinct values must still replace the slot (PR #355).
					return old;
				var mutableTail = makeMutable(tail);
				if (mutableTail != tail)
					bufferModCount++;
				mutableTail[tailIndex] = element;
				tail = mutableTail;
				built = null;
				return old;
			}
			var carry = new Ref(null);
			root = setInRoot(root, rootShift, index, element, carry);
			if (carry.value != element)
				built = null;
			return (E)carry.value;
		}

		private Object[] setInRoot(Object[] root, int shift, int index, E element, Ref carry) {
			int bufferIndex = segment(index, shift);
			if (shift == 0) {
				carry.value = root[bufferIndex];
				if (carry.value == element)
					return root;
				var mutableRoot = makeMutable(root);
				if (mutableRoot != root)
					bufferModCount++;
				mutableRoot[bufferIndex] = element;
				return mutableRoot;
			}
			// Descend before copying so a same-reference set leaves the entire path shared.
			var child = setInRoot((Object[])root[bufferIndex], shift - BITS, index, element, carry);
			if (carry.value == element)
				return root;
			var mutableRoot = makeMutable(root);
			mutableRoot[bufferIndex] = child;
			return mutableRoot;
		}

		@Override
		public void clear() {
			if (size == 0)
				return;
			modCount++;
			built = empty();
			root = null;
			tail = EMPTY_BUFFER;
			size = 0;
			rootShift = 0;
		}

		@Override
		public boolean removeAll(Collection<?> elements) {
			if (isEmpty() || elements.isEmpty())
				return false;
			if (elements == this) {
				//noinspection DataFlowIssue
				clear();
				return true;
			}
			return filter(elements::contains);
		}

		@Override
		public boolean retainAll(Collection<?> elements) {
			if (isEmpty() || elements == this)
				return false;
			if (elements.isEmpty()) {
				clear();
				return true;
			}
			return filter(element -> !elements.contains(element));
		}

		@Override
		public boolean removeIf(Predicate<? super E> predicate) {
			return filter(predicate);
		}

		@SuppressWarnings("unchecked")
		private boolean filter(Predicate<? super E> predicate) {
			// No snapshot, frozen buffers or construction state until the first match.
			int expectedModCount = modCount;
			var iterator = iterator();
			int index = 0;
			while (iterator.hasNext()) {
				boolean remove = predicate.test(iterator.next());
				if (modCount != expectedModCount)
					throw new ConcurrentModificationException();
				if (remove)
					break;
				index++;
			}
			if (index == size)
				return false;
			var source = build();
			var result = source.builder();
			result.truncate(index++);
			try {
				while (index < source.size) {
					var buffer = bufferFor(source.root, source.tail, source.size, source.rootShift, index);
					int offset = index & MASK;
					int limit = Math.min(WIDTH, source.size - (index & ~MASK));
					int runStart = offset;
					try {
						while (offset < limit) {
							boolean remove = predicate.test((E)buffer[offset]);
							if (modCount != expectedModCount)
								throw new ConcurrentModificationException();
							if (remove) {
								result.appendBuffer(buffer, runStart, offset - runStart, false);
								runStart = offset + 1;
							}
							offset++;
							index++;
						}
					} finally {
						if (modCount == expectedModCount)
							result.appendBuffer(buffer, runStart, offset - runStart, runStart == 0 && offset == WIDTH);
					}
				}
			} finally { // On failure retain the throwing element and untested suffix (PR #347).
				// A structurally modifying callback owns the live state; never overwrite it with an old snapshot.
				if (modCount == expectedModCount) {
					result.appendRange(source, index, source.size);
					adopt(result);
				}
			}
			return true;
		}

		private void adopt(Builder<E> result) {
			modCount++;
			built = result.built;
			ownership = result.ownership;
			root = result.root;
			tail = result.tail;
			size = result.size;
			rootShift = result.rootShift;
		}

		// Keep a prefix by trimming only the rightmost path, rather than removing its suffix one by one.
		private void truncate(int newSize) {
			if (newSize == size)
				return;
			built = null;
			if (newSize == 0) {
				root = null;
				tail = EMPTY_BUFFER;
				rootShift = 0;
			} else {
				int newRootSize = rootSize(newSize);
				int newTailSize = newSize - newRootSize;
				var buffer = bufferFor(root, tail, size, rootShift, newSize - 1);
				if (newTailSize == WIDTH)
					tail = buffer;
				else {
					tail = mutableBuffer();
					System.arraycopy(buffer, 0, tail, 0, newTailSize);
				}
				if (newRootSize == 0) {
					root = null;
					rootShift = 0;
				} else {
					int last = newRootSize - 1;
					while (rootShift > 0 && (last >>> rootShift) == 0) {
						root = (Object[])root[0];
						rootShift -= BITS;
					}
					root = trimRoot(root, rootShift, last);
				}
			}
			size = newSize;
		}

		private Object[] trimRoot(Object[] root, int shift, int last) {
			if (shift == 0)
				return root;
			int index = segment(last, shift);
			var mutableRoot = makeMutable(root);
			mutableRoot[index] = trimRoot((Object[])mutableRoot[index], shift - BITS, last);
			Arrays.fill(mutableRoot, index + 1, WIDTH, null);
			return mutableRoot;
		}

		@Override
		protected void removeRange(int fromIndex, int toIndex) {
			Objects.checkFromToIndex(fromIndex, toIndex, size);
			if (fromIndex == toIndex)
				return;
			if (fromIndex == 0 && toIndex == size) {
				clear();
				return;
			}
			var source = build();
			var result = source.builder();
			result.truncate(fromIndex);
			result.appendRange(source, toIndex, source.size);
			adopt(result);
		}

		@Override
		public Iterator<E> iterator() {
			return new BuilderIterator();
		}

		private final class BuilderIterator implements Iterator<E> {
			private int expectedModCount = modCount;
			private int expectedBufferModCount = bufferModCount;
			private int cursor;
			private int last = -1;
			private int leafIndex = -1;
			private Object[] leaf;

			private void checkModification() {
				if (expectedModCount != modCount)
					throw new ConcurrentModificationException();
			}

			@Override
			public boolean hasNext() {
				return cursor != size; // Leave a cursor beyond the live size for next() to report the CME (PR #337).
			}

			@SuppressWarnings("unchecked")
			@Override
			public E next() {
				checkModification();
				if (!hasNext())
					throw new NoSuchElementException();
				int newLeafIndex = cursor >>> BITS;
				if (leafIndex != newLeafIndex || expectedBufferModCount != bufferModCount) {
					leaf = bufferFor(root, tail, size, rootShift, cursor);
					leafIndex = newLeafIndex;
					expectedBufferModCount = bufferModCount;
				}
				last = cursor;
				return (E)leaf[cursor++ & MASK];
			}

			@Override
			public void remove() {
				if (last < 0)
					throw new IllegalStateException();
				checkModification();
				Builder.this.remove(last);
				cursor = last;
				last = -1;
				expectedModCount = modCount;
				leafIndex = -1;
				leaf = null;
			}
		}
	}
}
