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
 * Java adaptation of kotlinx.collections.immutable 0.5.2: PersistentHashSet,
 * PersistentHashSetBuilder, TrieNode and PersistentHashSetIterator.
 * Persistent and mutable updates share algorithms, using a null owner for
 * persistent path copying. Bulk operations use individual updates rather than
 * general trie merging, with a fast union of disjoint root bitmaps. Unused
 * retainAll, predicate removal and mutable iterators are
 * not ported. Iteration uses fixed-depth arrays instead of a list of cursors.
 */
package Zeze.Transaction.Collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Immutable 32-way hash trie with one bitmap and a compact mixed element/node
 * buffer. Full hash collisions occupy a flat leaf beyond bit 30. Updates share
 * unchanged nodes and preserve the receiver's equal element instance.
 *
 * <p>API needed by PSet/PSet1 and LogSet/LogSet1: empty, adding, addingAll,
 * removing, removingAll, builder; Builder.build/add/addAll/remove/removeAll/clear
 * and Set reads. Standard reads, equality and hashing come from AbstractSet.
 * Persistent mutators and iterator.remove are unsupported via the JDK defaults;
 * no-op mutators may return normally. Builder iterators are read-only snapshots,
 * so inherited retainAll/removeIf cannot remove elements through an iterator.
 *
 * <p>Builders edit only nodes tagged with their ownership token. build() changes
 * that token before further writes. Elements themselves are shared, including
 * null (PSet still rejects null). Builders are not thread safe.
 */
public final class PersistentHashSet<E> extends AbstractSet<E> {
	private static final int BITS = 5;
	private static final int MAX_SHIFT = 30;
	private static final Node EMPTY_NODE = new Node(0, new Object[0], null);
	private static final PersistentHashSet<?> EMPTY = new PersistentHashSet<>(EMPTY_NODE, 0);

	private final Node root;
	private final int size;

	private PersistentHashSet(Node root, int size) {
		this.root = root;
		this.size = size;
	}

	@SuppressWarnings("unchecked")
	public static <E> PersistentHashSet<E> empty() {
		return (PersistentHashSet<E>)EMPTY;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean contains(Object element) {
		return root.contains(Objects.hashCode(element), element, 0);
	}

	@Override
	public Iterator<E> iterator() {
		return new SetIterator<>(root);
	}

	public Builder<E> builder() {
		return new Builder<>(this);
	}

	public PersistentHashSet<E> adding(E element) {
		var newRoot = root.add(Objects.hashCode(element), element, 0, null);
		return root == newRoot ? this : new PersistentHashSet<>(newRoot, size + 1);
	}

	public PersistentHashSet<E> removing(Object element) {
		var newRoot = root.remove(Objects.hashCode(element), element, 0, null);
		return root == newRoot ? this : size == 1 ? empty() : new PersistentHashSet<>(newRoot, size - 1);
	}

	public PersistentHashSet<E> addingAll(Collection<? extends E> elements) {
		if (elements == this || elements.isEmpty())
			return this;
		var builder = builder();
		builder.addAll(elements);
		return builder.build();
	}

	public PersistentHashSet<E> removingAll(Collection<?> elements) {
		if (elements == this)
			return empty();
		if (isEmpty() || elements.isEmpty())
			return this;
		var builder = builder();
		//noinspection SuspiciousMethodCalls
		builder.removeAll(elements);
		return builder.build();
	}

	public static final class Builder<E> extends AbstractSet<E> {
		private PersistentHashSet<E> builtSet;
		private Object ownership = new Object();
		private Node root;
		private int size;

		private Builder(PersistentHashSet<E> set) {
			builtSet = set;
			root = set.root;
			size = set.size;
		}

		public PersistentHashSet<E> build() {
			if (builtSet == null) {
				builtSet = size == 0 ? empty() : new PersistentHashSet<>(root, size);
				ownership = new Object();
			}
			return builtSet;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object element) {
			return root.contains(Objects.hashCode(element), element, 0);
		}

		@Override
		public Iterator<E> iterator() {
			return build().iterator();
		}

		@Override
		public boolean add(E element) {
			int oldSize = size;
			root = root.add(Objects.hashCode(element), element, 0, this);
			if (size == oldSize)
				return false;
			builtSet = null;
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends E> elements) {
			if (elements == this)
				return false;
			if (elements instanceof PersistentHashSet<?> set && (root.bitmap & set.root.bitmap) == 0) {
				if (set.size == 0)
					return false;
				// Disjoint first hash segments cannot contain equal elements. Reuse
				// their immutable subtries without equality/hash callbacks or rehashing.
				root = root.unionDisjoint(set.root, ownership);
				size += set.size;
				builtSet = null;
				return true;
			}
			boolean changed = false;
			for (E element : elements)
				changed |= add(element);
			return changed;
		}

		@Override
		public boolean remove(Object element) {
			int oldSize = size;
			root = root.remove(Objects.hashCode(element), element, 0, this);
			if (size == oldSize)
				return false;
			builtSet = null;
			return true;
		}

		@Override
		public boolean removeAll(Collection<?> elements) {
			if (elements == this) {
				boolean changed = size != 0;
				clear();
				return changed;
			}
			boolean changed = false;
			for (Object element : elements)
				changed |= remove(element);
			return changed;
		}

		@Override
		public void clear() {
			if (size != 0) {
				root = EMPTY_NODE;
				size = 0;
				builtSet = null;
			}
		}
	}

	private static final class Node {
		private int bitmap;
		private Object[] buffer;
		private final Object ownedBy;

		private Node(int bitmap, Object[] buffer, Object ownedBy) {
			this.bitmap = bitmap;
			this.buffer = buffer;
			this.ownedBy = ownedBy;
		}

		private int index(int bit) {
			return Integer.bitCount(bitmap & (bit - 1));
		}

		private int collisionIndex(Object element) {
			for (int i = 0; i < buffer.length; i++) {
				if (Objects.equals(element, buffer[i]))
					return i;
			}
			return -1;
		}

		private boolean contains(int hash, Object element, int shift) {
			int bit = 1 << ((hash >>> shift) & 31);
			if ((bitmap & bit) == 0)
				return false;
			var cell = buffer[index(bit)];
			if (!(cell instanceof Node node))
				return Objects.equals(element, cell);
			return shift == MAX_SHIFT ? node.collisionIndex(element) >= 0
					: node.contains(hash, element, shift + BITS);
		}

		private Node properties(int bits, Object[] cells, Object owner) {
			if (owner != null && ownedBy == owner) {
				bitmap = bits;
				buffer = cells;
				return this;
			}
			return new Node(bits, cells, owner);
		}

		private Node cell(int i, Object value, Object owner) {
			if (owner != null && ownedBy == owner) {
				buffer[i] = value;
				return this;
			}
			var cells = buffer.clone();
			cells[i] = value;
			return new Node(bitmap, cells, owner);
		}

		private Node insert(int i, int bit, Object element, Object owner) {
			var cells = new Object[buffer.length + 1];
			System.arraycopy(buffer, 0, cells, 0, i);
			cells[i] = element;
			System.arraycopy(buffer, i, cells, i + 1, buffer.length - i);
			return properties(bitmap | bit, cells, owner);
		}

		private Node unionDisjoint(Node other, Object owner) {
			int bits = bitmap | other.bitmap;
			var cells = new Object[buffer.length + other.buffer.length];
			int left = 0;
			int right = 0;
			int i = 0;
			for (int rest = bits; rest != 0; ) {
				int bit = Integer.lowestOneBit(rest);
				cells[i++] = (bitmap & bit) != 0 ? buffer[left++] : other.buffer[right++];
				rest ^= bit;
			}
			return properties(bits, cells, owner);
		}

		private Node delete(int i, int bit, Object owner) {
			if (buffer.length == 1)
				return EMPTY_NODE;
			var cells = new Object[buffer.length - 1];
			System.arraycopy(buffer, 0, cells, 0, i);
			System.arraycopy(buffer, i + 1, cells, i, cells.length - i);
			return properties(bitmap ^ bit, cells, owner);
		}

		private static Node makeNode(int hash1, Object element1, int hash2, Object element2,
									 int shift, Object owner) {
			if (shift > MAX_SHIFT)
				return new Node(0, new Object[]{element1, element2}, owner);
			int segment1 = (hash1 >>> shift) & 31;
			int segment2 = (hash2 >>> shift) & 31;
			if (segment1 != segment2) {
				return new Node((1 << segment1) | (1 << segment2), segment1 < segment2
						? new Object[]{element1, element2} : new Object[]{element2, element1}, owner);
			}
			return new Node(1 << segment1, new Object[]{makeNode(hash1, element1,
					hash2, element2, shift + BITS, owner)}, owner);
		}

		private Node add(int hash, Object element, int shift, Builder<?> builder) {
			Object owner = builder != null ? builder.ownership : null;
			int bit = 1 << ((hash >>> shift) & 31);
			int i = index(bit);
			Node result;
			if ((bitmap & bit) == 0)
				result = insert(i, bit, element, owner);
			else {
				var stored = buffer[i];
				if (stored instanceof Node node) {
					var newNode = shift == MAX_SHIFT ? node.collisionAdd(element, builder)
							: node.add(hash, element, shift + BITS, builder);
					return node == newNode ? this : cell(i, newNode, owner);
				}
				if (Objects.equals(element, stored))
					return this;
				// #374: hashCode of the stored element may throw. Count only after it succeeds.
				var newNode = makeNode(Objects.hashCode(stored), stored, hash, element, shift + BITS, owner);
				result = cell(i, newNode, owner);
			}
			if (builder != null)
				builder.size++;
			return result;
		}

		private Node collisionAdd(Object element, Builder<?> builder) {
			if (collisionIndex(element) >= 0)
				return this;
			var result = insert(0, 0, element, builder != null ? builder.ownership : null);
			if (builder != null)
				builder.size++;
			return result;
		}

		private Node remove(int hash, Object element, int shift, Builder<?> builder) {
			int bit = 1 << ((hash >>> shift) & 31);
			if ((bitmap & bit) == 0)
				return this;
			int i = index(bit);
			Object owner = builder != null ? builder.ownership : null;
			var stored = buffer[i];
			if (stored instanceof Node node) {
				var newNode = shift == MAX_SHIFT ? node.collisionRemove(element, builder)
						: node.remove(hash, element, shift + BITS, builder);
				if (node == newNode && !(newNode.buffer.length == 1 && !(newNode.buffer[0] instanceof Node)))
					return this;
				return canonicalize(i, bit, newNode, owner);
			}
			if (!Objects.equals(element, stored))
				return this;
			var result = delete(i, bit, owner);
			if (builder != null)
				builder.size--;
			return result;
		}

		private Node collisionRemove(Object element, Builder<?> builder) {
			int i = collisionIndex(element);
			if (i < 0)
				return this;
			var result = delete(i, 0, builder != null ? builder.ownership : null);
			if (builder != null)
				builder.size--;
			return result;
		}

		private Node canonicalize(int i, int bit, Node node, Object owner) {
			if (node.buffer.length == 0)
				return delete(i, bit, owner);
			if (node.buffer.length == 1 && !(node.buffer[0] instanceof Node)) {
				if (buffer.length == 1)
					return new Node(bitmap, node.buffer, owner);
				return cell(i, node.buffer[0], owner);
			}
			return cell(i, node, owner);
		}
	}

	private static final class SetIterator<E> implements Iterator<E> {
		private final Node[] path = new Node[8];
		private final int[] indices = new int[8];
		private int depth;

		private SetIterator(Node root) {
			path[0] = root;
			advance();
		}

		private void advance() {
			while (depth >= 0) {
				var node = path[depth];
				int i = indices[depth];
				if (i == node.buffer.length)
					depth--;
				else if (node.buffer[i] instanceof Node child) {
					indices[depth]++;
					path[++depth] = child;
					indices[depth] = 0;
				} else
					return;
			}
		}

		@Override
		public boolean hasNext() {
			return depth >= 0;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			var element = (E)path[depth].buffer[indices[depth]++];
			advance();
			return element;
		}
	}
}
