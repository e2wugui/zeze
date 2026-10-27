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
 * Java adaptation of kotlinx.collections.immutable 0.5.2: PersistentHashMap,
 * PersistentHashMapBuilder, TrieNode, PersistentHashMapContentViews and
 * PersistentHashMapContentIterators.
 * Persistent and builder updates share the copy-on-write node algorithms.
 * Bulk writes merge disjoint roots or use repeated mutablePut; general trie merging, mutable iterators,
 * ordered-map interoperability and specialized equality are omitted.
 */
package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable 32-way hash trie. Entries occupy the front of each node buffer;
 * child nodes occupy its end in reverse bitmap order. Beyond the 32 hash bits,
 * equal-hash keys are stored in a flat collision buffer.
 *
 * <p>Only the API used by PMap/PMap1/PMap2 and LogMap/LogMap1/LogMap2 is ported:
 * empty, putting, puttingAll, removing, builder; Builder.build/put/putAll/remove/
 * clear and standard Map reads. Read views and entries are unmodifiable, including
 * builder views (iteration takes a snapshot). JDK AbstractMap supplies remaining
 * reads, equality and hashing. Null keys/values are supported by the trie;
 * PMap continues to reject them.
 * Key and value views (keySet/keys and values) traverse trie buffers directly,
 * without allocating intermediate entries. All view iterators use the same order.
 *
 * <p>Builders edit only nodes carrying their ownership token. build() changes
 * that token before further writes, protecting all previously built snapshots.
 * Keys and values themselves are shared. Builders are not thread safe.
 */
public final class PersistentHashMap<K, V> extends AbstractMap<K, V> {
	private static final int BITS = 5;
	private static final int MAX_SHIFT = 30;
	private static final Node EMPTY_NODE = new Node(0, 0, new Object[0], null);
	private static final PersistentHashMap<?, ?> EMPTY = new PersistentHashMap<>(EMPTY_NODE, 0);

	private final Node root;
	private final int size;

	private PersistentHashMap(Node root, int size) {
		this.root = root;
		this.size = size;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> PersistentHashMap<K, V> empty() {
		return (PersistentHashMap<K, V>)EMPTY;
	}

	@Override
	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		return (V)root.get(Objects.hashCode(key), key, 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return root.containsKey(Objects.hashCode(key), key, 0);
	}

	@Override
	public boolean containsValue(Object value) {
		return root.containsValue(value);
	}

	public Builder<K, V> builder() {
		return new Builder<>(this);
	}

	public PersistentHashMap<K, V> putting(K key, V value) {
		var builder = builder();
		builder.put(key, value);
		return builder.build();
	}

	public PersistentHashMap<K, V> puttingAll(Map<? extends K, ? extends V> map) {
		if (map == this || map.isEmpty())
			return this;
		var builder = builder();
		builder.putAll(map);
		return builder.build();
	}

	public PersistentHashMap<K, V> removing(Object key) {
		var builder = builder();
		//noinspection SuspiciousMethodCalls
		builder.remove(key);
		return builder.build();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new Entries<>(this);
	}

	@Override
	public Set<K> keySet() {
		return new Keys<>(this);
	}

	public Set<K> keys() {
		return keySet();
	}

	@Override
	public Collection<V> values() {
		return new Values<>(this);
	}

	private static final class Keys<K> extends AbstractSet<K> {
		private final PersistentHashMap<K, ?> map;

		private Keys(PersistentHashMap<K, ?> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size;
		}

		@Override
		public boolean contains(Object key) {
			//noinspection SuspiciousMethodCalls
			return map.containsKey(key);
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator<>(map.root);
		}
	}

	private static final class Values<V> extends AbstractCollection<V> {
		private final PersistentHashMap<?, V> map;

		private Values(PersistentHashMap<?, V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size;
		}

		@Override
		public boolean contains(Object value) {
			//noinspection SuspiciousMethodCalls
			return map.containsValue(value);
		}

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator<>(map.root);
		}
	}

	// One object, as in Kotlin's PersistentHashMapEntries. Keep the snapshot map
	// rather than capturing root/size and allocating a second unmodifiable wrapper.
	private static final class Entries<K, V> extends AbstractSet<Entry<K, V>> {
		private final PersistentHashMap<K, V> map;

		private Entries(PersistentHashMap<K, V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size;
		}

		@Override
		public boolean contains(Object item) {
			if (!(item instanceof Entry<?, ?> entry))
				return false;
			var key = entry.getKey();
			int hash = Objects.hashCode(key);
			var value = map.root.get(hash, key, 0);
			return Objects.equals(entry.getValue(), value)
					&& (value != null || map.root.containsKey(hash, key, 0));
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIterator<>(map.root);
		}
	}

	public static final class Builder<K, V> extends AbstractMap<K, V> {
		private PersistentHashMap<K, V> builtMap;
		private Object ownership = new Object();
		private Node root;
		private int size;
		private V operationResult;
		private boolean changed;

		private Builder(PersistentHashMap<K, V> map) {
			builtMap = map;
			root = map.root;
			size = map.size;
		}

		public PersistentHashMap<K, V> build() {
			if (builtMap == null) {
				builtMap = size == 0 ? empty() : new PersistentHashMap<>(root, size);
				ownership = new Object();
			}
			return builtMap;
		}

		@Override
		public int size() {
			return size;
		}

		@SuppressWarnings("unchecked")
		@Override
		public V get(Object key) {
			return (V)root.get(Objects.hashCode(key), key, 0);
		}

		@Override
		public boolean containsKey(Object key) {
			return root.containsKey(Objects.hashCode(key), key, 0);
		}

		@Override
		public boolean containsValue(Object value) {
			return root.containsValue(value);
		}

		@Override
		public V put(K key, V value) {
			operationResult = null;
			changed = false;
			root = root.put(Objects.hashCode(key), key, value, 0, this);
			if (changed)
				builtMap = null;
			return operationResult;
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> map) {
			if (map == this)
				return;
			if (map instanceof PersistentHashMap<?, ?> other
					&& ((root.dataMap | root.nodeMap) & (other.root.dataMap | other.root.nodeMap)) == 0) {
				if (other.size != 0) {
					root = root.unionDisjoint(other.root, ownership);
					size += other.size;
					builtMap = null;
				}
				return;
			}
			for (var e : map.entrySet())
				put(e.getKey(), e.getValue());
		}

		@Override
		public V remove(Object key) {
			operationResult = null;
			changed = false;
			var newRoot = root.remove(Objects.hashCode(key), key, 0, this);
			root = newRoot != null ? newRoot : EMPTY_NODE;
			if (changed)
				builtMap = null;
			return operationResult;
		}

		@Override
		public void clear() {
			if (size != 0) {
				root = EMPTY_NODE;
				size = 0;
				builtMap = null;
			}
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return build().entrySet();
		}

		@Override
		public Set<K> keySet() {
			return build().keySet();
		}

		public Set<K> keys() {
			return keySet();
		}

		@Override
		public Collection<V> values() {
			return build().values();
		}
	}

	private static final class Node {
		private int dataMap;
		private int nodeMap;
		private Object[] buffer;
		private final Object ownedBy;

		private Node(int dataMap, int nodeMap, Object[] buffer, Object ownedBy) {
			this.dataMap = dataMap;
			this.nodeMap = nodeMap;
			this.buffer = buffer;
			this.ownedBy = ownedBy;
		}

		private int entryIndex(int bit) {
			return Integer.bitCount(dataMap & (bit - 1)) * 2;
		}

		private int nodeIndex(int bit) {
			return buffer.length - 1 - Integer.bitCount(nodeMap & (bit - 1));
		}

		private Node child(int index) {
			return (Node)buffer[index];
		}

		private boolean singleEntry() {
			return buffer.length == 2 && nodeMap == 0;
		}

		// Test key presence independently of its value, including present-null values.
		private boolean containsKey(int hash, Object key, int shift) {
			if (shift > MAX_SHIFT) {
				for (int i = 0; i < buffer.length; i += 2) {
					if (Objects.equals(key, buffer[i]))
						return true;
				}
				return false;
			}
			int bit = 1 << ((hash >>> shift) & 31);
			if ((dataMap & bit) != 0) {
				int i = entryIndex(bit);
				return Objects.equals(key, buffer[i]);
			}
			return (nodeMap & bit) != 0 && child(nodeIndex(bit)).containsKey(hash, key, shift + BITS);
		}

		private Object get(int hash, Object key, int shift) {
			if (shift > MAX_SHIFT) {
				for (int i = 0; i < buffer.length; i += 2) {
					if (Objects.equals(key, buffer[i]))
						return buffer[i + 1];
				}
				return null;
			}
			int bit = 1 << ((hash >>> shift) & 31);
			if ((dataMap & bit) != 0) {
				int i = entryIndex(bit);
				return Objects.equals(key, buffer[i]) ? buffer[i + 1] : null;
			}
			return (nodeMap & bit) != 0 ? child(nodeIndex(bit)).get(hash, key, shift + BITS) : null;
		}

		private boolean containsValue(Object value) {
			// A collision leaf has no bitmap; all of its buffer consists of entries.
			int dataSize = nodeMap == 0 ? buffer.length : Integer.bitCount(dataMap) * 2;
			for (int i = 1; i < dataSize; i += 2) {
				if (Objects.equals(value, buffer[i]))
					return true;
			}
			for (int i = dataSize; i < buffer.length; i++) {
				if (child(i).containsValue(value))
					return true;
			}
			return false;
		}

		private Node editable(Object owner) {
			return ownedBy == owner ? this : new Node(dataMap, nodeMap, buffer.clone(), owner);
		}

		private Node withBuffer(int data, int nodes, Object[] newBuffer, Object owner) {
			if (ownedBy != owner)
				return new Node(data, nodes, newBuffer, owner);
			dataMap = data;
			nodeMap = nodes;
			buffer = newBuffer;
			return this;
		}

		// Root slots are disjoint: merge entries in ascending bitmap order and
		// child nodes in reverse order. Only immutable source subtrees are shared.
		private Node unionDisjoint(Node other, Object owner) {
			int data = dataMap | other.dataMap;
			int nodes = nodeMap | other.nodeMap;
			var merged = new Object[buffer.length + other.buffer.length];
			int index = 0;
			for (int bits = data; bits != 0; bits &= bits - 1) {
				int bit = Integer.lowestOneBit(bits);
				var source = (dataMap & bit) != 0 ? this : other;
				int i = source.entryIndex(bit);
				merged[index++] = source.buffer[i];
				merged[index++] = source.buffer[i + 1];
			}
			index = merged.length;
			for (int bits = nodes; bits != 0; bits &= bits - 1) {
				int bit = Integer.lowestOneBit(bits);
				var source = (nodeMap & bit) != 0 ? this : other;
				merged[--index] = source.child(source.nodeIndex(bit));
			}
			return withBuffer(data, nodes, merged, owner);
		}

		private static Object[] insert(Object[] buffer, int i, Object key, Object value) {
			var result = new Object[buffer.length + 2];
			System.arraycopy(buffer, 0, result, 0, i);
			result[i] = key;
			result[i + 1] = value;
			System.arraycopy(buffer, i, result, i + 2, buffer.length - i);
			return result;
		}

		private static Object[] delete(Object[] buffer, int i, int count) {
			var result = new Object[buffer.length - count];
			System.arraycopy(buffer, 0, result, 0, i);
			System.arraycopy(buffer, i + count, result, i, result.length - i);
			return result;
		}

		private static Node makeNode(int hash1, Object key1, Object value1,
									 int hash2, Object key2, Object value2, int shift, Object owner) {
			if (shift > MAX_SHIFT)
				return new Node(0, 0, new Object[]{key1, value1, key2, value2}, owner);
			int segment1 = (hash1 >>> shift) & 31;
			int segment2 = (hash2 >>> shift) & 31;
			if (segment1 != segment2) {
				return new Node((1 << segment1) | (1 << segment2), 0, segment1 < segment2
						? new Object[]{key1, value1, key2, value2}
						: new Object[]{key2, value2, key1, value1}, owner);
			}
			return new Node(0, 1 << segment1, new Object[]{makeNode(hash1, key1, value1,
					hash2, key2, value2, shift + BITS, owner)}, owner);
		}

		@SuppressWarnings("unchecked")
		private <K, V> Node put(int hash, K key, V value, int shift, Builder<K, V> builder) {
			var owner = builder.ownership;
			int bit = shift <= MAX_SHIFT ? 1 << ((hash >>> shift) & 31) : 0;
			int i = entryIndex(bit);
			if (shift > MAX_SHIFT || (dataMap & bit) != 0) {
				if (shift > MAX_SHIFT) {
					for (i = 0; i < buffer.length; i += 2) {
						if (Objects.equals(key, buffer[i]))
							break;
					}
				}
				if (i < buffer.length && (shift > MAX_SHIFT || Objects.equals(key, buffer[i]))) {
					builder.operationResult = (V)buffer[i + 1];
					if (buffer[i + 1] == value)
						return this;
					builder.changed = true;
					var result = editable(owner);
					result.buffer[i + 1] = value;
					return result;
				}
				if (shift > MAX_SHIFT) {
					builder.changed = true;
					builder.size++;
					return withBuffer(0, 0, insert(buffer, 0, key, value), owner);
				}
				// #374: hashing the stored key can throw. Prepare the whole replacement
				// before publishing any size/change metadata or modifying owned nodes.
				var node = makeNode(Objects.hashCode(buffer[i]), buffer[i], buffer[i + 1],
						hash, key, value, shift + BITS, owner);
				// Remove the entry and insert its subtrie into the reversed node area.
				int nodeIndex = nodeIndex(bit) - 1;
				var newBuffer = new Object[buffer.length - 1];
				System.arraycopy(buffer, 0, newBuffer, 0, i);
				System.arraycopy(buffer, i + 2, newBuffer, i, nodeIndex - i);
				newBuffer[nodeIndex] = node;
				System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1,
						buffer.length - nodeIndex - 2);
				builder.changed = true;
				builder.size++;
				return withBuffer(dataMap ^ bit, nodeMap | bit, newBuffer, owner);
			}
			if ((nodeMap & bit) != 0) {
				int index = nodeIndex(bit);
				var old = child(index);
				var node = old.put(hash, key, value, shift + BITS, builder);
				if (old == node)
					return this;
				var result = editable(owner);
				result.buffer[index] = node;
				return result;
			}
			builder.changed = true;
			builder.size++;
			return withBuffer(dataMap | bit, nodeMap, insert(buffer, i, key, value), owner);
		}

		@SuppressWarnings("unchecked")
		private <K, V> Node remove(int hash, Object key, int shift, Builder<K, V> builder) {
			var owner = builder.ownership;
			int bit = shift <= MAX_SHIFT ? 1 << ((hash >>> shift) & 31) : 0;
			if (shift > MAX_SHIFT || (dataMap & bit) != 0) {
				int i = entryIndex(bit);
				if (shift > MAX_SHIFT) {
					for (i = 0; i < buffer.length; i += 2) {
						if (Objects.equals(key, buffer[i]))
							break;
					}
				}
				if (i == buffer.length || (shift <= MAX_SHIFT && !Objects.equals(key, buffer[i])))
					return this;
				builder.operationResult = (V)buffer[i + 1];
				builder.changed = true;
				builder.size--;
				return buffer.length == 2 ? null
						: withBuffer(dataMap ^ bit, nodeMap, delete(buffer, i, 2), owner);
			}
			if ((nodeMap & bit) == 0)
				return this;
			int index = nodeIndex(bit);
			var old = child(index);
			var node = old.remove(hash, key, shift + BITS, builder);
			if (node == null) {
				return buffer.length == 1 ? null
						: withBuffer(dataMap, nodeMap ^ bit, delete(buffer, index, 1), owner);
			}
			if (node.singleEntry()) {
				// Promote the last entry, including a collision node that shrank to one.
				if (buffer.length == 1)
					return new Node(bit, 0, node.buffer, owner);
				int i = entryIndex(bit);
				var newBuffer = Arrays.copyOf(buffer, buffer.length + 1);
				System.arraycopy(buffer, index + 1, newBuffer, index + 2, buffer.length - index - 1);
				System.arraycopy(buffer, i, newBuffer, i + 2, index - i);
				newBuffer[i] = node.buffer[0];
				newBuffer[i + 1] = node.buffer[1];
				return withBuffer(dataMap | bit, nodeMap ^ bit, newBuffer, owner);
			}
			if (node == old)
				return this;
			var result = editable(owner);
			result.buffer[index] = node;
			return result;
		}
	}

	// All three iterators traverse exactly the same buffers and order. Key/value
	// projections return the stored object directly without constructing an Entry.
	private abstract static class TrieIterator<T> implements Iterator<T> {
		private final Node[] path = new Node[8];
		private final int[] indices = new int[8];
		private int depth;

		private TrieIterator(Node root) {
			path[0] = root;
			advance();
		}

		private void advance() {
			while (depth >= 0) {
				var node = path[depth];
				int dataSize = depth == 7 ? node.buffer.length : Integer.bitCount(node.dataMap) * 2;
				int index = indices[depth];
				if (index < dataSize)
					return;
				if (index < node.buffer.length) {
					indices[depth]++;
					path[++depth] = node.child(index);
					indices[depth] = 0;
				} else
					depth--;
			}
		}

		@Override
		public final boolean hasNext() {
			return depth >= 0;
		}

		@Override
		public final T next() {
			if (!hasNext())
				throw new NoSuchElementException();
			var buffer = path[depth].buffer;
			int i = indices[depth];
			var result = elementAt(buffer, i);
			indices[depth] += 2;
			advance();
			return result;
		}

		protected abstract T elementAt(Object[] buffer, int index);
	}

	private static final class EntryIterator<K, V> extends TrieIterator<Entry<K, V>> {
		private EntryIterator(Node root) {
			super(root);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Entry<K, V> elementAt(Object[] buffer, int index) {
			return new SimpleImmutableEntry<>((K)buffer[index], (V)buffer[index + 1]);
		}
	}

	private static final class KeyIterator<K> extends TrieIterator<K> {
		private KeyIterator(Node root) {
			super(root);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected K elementAt(Object[] buffer, int index) {
			return (K)buffer[index];
		}
	}

	private static final class ValueIterator<V> extends TrieIterator<V> {
		private ValueIterator(Node root) {
			super(root);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected V elementAt(Object[] buffer, int index) {
			return (V)buffer[index + 1];
		}
	}
}
