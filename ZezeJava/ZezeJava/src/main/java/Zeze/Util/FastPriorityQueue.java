package Zeze.Util;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 基于最小堆的优先级队列，支持 O(1) {@link #contains} 与 O(log n) 任意节点删除/优先级更新。
 * 参考 <a href="https://github.com/BlueRaja/High-Speed-Priority-Queue-for-C-Sharp/wiki/Getting-Started">High-Speed-Priority-Queue</a>。
 *
 * @param <T> 队列节点类型，需实现 {@link FastPriorityQueueNode}
 */
public class FastPriorityQueue<T extends FastPriorityQueueNode<T>> implements Iterable<T> {
	private final int maxCapacity;
	private int numNodes;
	private @Nullable T[] nodes;

	/**
	 * 构造优先级队列。
	 *
	 * @param maxCapacity 允许入队的最大节点数
	 */
	@SuppressWarnings("unchecked")
	public FastPriorityQueue(int initialCapacity, int maxCapacity) {
		if (initialCapacity > maxCapacity)
			throw new IllegalArgumentException("initialCapacity(" + initialCapacity + ") > maxCapacity(" + maxCapacity + ')');
		this.maxCapacity = maxCapacity;
		nodes = (T[])new FastPriorityQueueNode[initialCapacity];
	}

	/** 返回当前队列中的节点数，O(1)。 */
	public int count() {
		return numNodes;
	}

	/** 返回当前底层数组容量，O(1)。 */
	public int maxSize() {
		return nodes.length;
	}

	/** 清空队列中的所有节点，O(n)。 */
	public void clear() {
		Arrays.fill(nodes, 0, numNodes, null);
		numNodes = 0;
	}

	/** 判断给定节点是否在队列中，O(1)。 */
	public boolean contains(T node) {
		int i = node.getQueueIndex();
		return i >= 0 && i < numNodes && nodes[i] == node;
	}

	/** 入队节点（优先级高者靠前，不可重复入队），O(log n)。 */
	public void enqueue(T node) {
		int i = numNodes;
		ensureCapacity(i + 1);
		node.setQueueIndex(i);
		nodes[i] = node;
		numNodes = i + 1;
		cascadeUp(node);
	}

	private void cascadeUp(final T node) {
		int i = node.getQueueIndex();
		if (i <= 0)
			return;
		@Nullable T[] nodes = this.nodes;
		int parent = (i - 1) >> 1;
		T parentNode = Objects.requireNonNull(nodes[parent]);
		if (!node.hasHigherPriority(parentNode))
			return;

		do {
			parentNode.setQueueIndex(i);
			nodes[i] = parentNode;
			i = parent;

			if (parent == 0)
				break;
			parent = (parent - 1) >> 1;
			parentNode = Objects.requireNonNull(nodes[parent]);
		} while (node.hasHigherPriority(parentNode));

		node.setQueueIndex(i);
		nodes[i] = node;
	}

	private void cascadeDown(T node) {
		int finalQueueIndex = node.getQueueIndex();
		int childLeftIndex = (finalQueueIndex << 1) + 1;

		int numNodes = this.numNodes;
		if (childLeftIndex >= numNodes)
			return;

		@Nullable T[] nodes = this.nodes;
		int childRightIndex = childLeftIndex + 1;
		T childLeft = nodes[childLeftIndex];
		//noinspection DataFlowIssue
		if (childLeft.hasHigherPriority(node)) {
			if (childRightIndex >= numNodes) {
				node.setQueueIndex(childLeftIndex);
				childLeft.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childLeft;
				nodes[childLeftIndex] = node;
				return;
			}

			T childRight = Objects.requireNonNull(nodes[childRightIndex]);
			if (childLeft.hasHigherPriority(childRight)) {
				childLeft.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childLeft;
				finalQueueIndex = childLeftIndex;
			} else {
				childRight.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childRight;
				finalQueueIndex = childRightIndex;
			}
		} else if (childRightIndex >= numNodes)
			return;
		else {
			T childRight = nodes[childRightIndex];
			//noinspection DataFlowIssue
			if (childRight.hasHigherPriority(node)) {
				childRight.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childRight;
				finalQueueIndex = childRightIndex;
			} else
				return;
		}

		for (; ; ) {
			childLeftIndex = (finalQueueIndex << 1) + 1;

			if (childLeftIndex >= numNodes) {
				node.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = node;
				break;
			}

			childRightIndex = childLeftIndex + 1;
			childLeft = nodes[childLeftIndex];
			//noinspection DataFlowIssue
			if (childLeft.hasHigherPriority(node)) {
				if (childRightIndex >= numNodes) {
					node.setQueueIndex(childLeftIndex);
					childLeft.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childLeft;
					nodes[childLeftIndex] = node;
					break;
				}

				T childRight = Objects.requireNonNull(nodes[childRightIndex]);
				if (childLeft.hasHigherPriority(childRight)) {
					childLeft.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childLeft;
					finalQueueIndex = childLeftIndex;
				} else {
					childRight.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childRight;
					finalQueueIndex = childRightIndex;
				}
			} else if (childRightIndex >= numNodes) {
				node.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = node;
				break;
			} else {
				T childRight = nodes[childRightIndex];
				//noinspection DataFlowIssue
				if (childRight.hasHigherPriority(node)) {
					childRight.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childRight;
					finalQueueIndex = childRightIndex;
				} else {
					node.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = node;
					break;
				}
			}
		}
	}

	/** 移除并返回队头节点（队列为空时行为未定义），O(log n)。 */
	public T dequeue() {
		@Nullable T[] nodes = this.nodes;
		T returnMe = Objects.requireNonNull(nodes[0]);
		int numNodes = this.numNodes;
		this.numNodes = --numNodes;
		if (numNodes == 0)
			nodes[0] = null;
		else {
			T formerLastNode = nodes[numNodes];
			//noinspection DataFlowIssue
			formerLastNode.setQueueIndex(0);
			nodes[0] = formerLastNode;
			nodes[numNodes] = null;

			cascadeDown(formerLastNode);
		}
		return returnMe;
	}

	/** 调整队列底层数组容量（不得小于当前节点数），O(n)。 */
	public void resize(int maxNodes) {
		nodes = Arrays.copyOf(nodes, maxNodes);
	}

	public void ensureCapacity(int capacity) {
		int n = nodes.length;
		if (n < capacity) {
			if (capacity > maxCapacity)
				throw new IllegalArgumentException("capacity(" + capacity + ") > maxCapacity(" + maxCapacity + ')');
			resize(Math.clamp((long)n << 1, capacity, maxCapacity));
		}
	}

	/** 返回队头节点但不移除（空队列返回 null），O(1)。 */
	public @Nullable T first() {
		return nodes[0];
	}

	/** 节点在队列内优先级变化后必须调用此方法维护堆序，O(log n)。 */
	public void updatePriority(T node) {
		onNodeUpdated(node);
	}

	private void onNodeUpdated(T node) {
		int parentIndex = (node.getQueueIndex() - 1) >> 1;
		if (parentIndex >= 0 && node.hasHigherPriority(Objects.requireNonNull(nodes[parentIndex])))
			cascadeUp(node);
		else
			cascadeDown(node);
	}

	/** 移除队列中指定节点（调用前须确保节点在队列内），O(log n)。 */
	public void remove(final T node) {
		@Nullable T[] nodes = this.nodes;
		int numNodes = this.numNodes;
		this.numNodes = --numNodes;
		int i = node.getQueueIndex();
		if (i == numNodes)
			nodes[numNodes] = null;
		else {
			T formerLastNode = Objects.requireNonNull(nodes[numNodes]);
			nodes[i] = formerLastNode;
			formerLastNode.setQueueIndex(i);
			nodes[numNodes] = null;

			onNodeUpdated(formerLastNode);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<>() {
			private int i;

			@Override
			public boolean hasNext() {
				return i < numNodes;
			}

			@Override
			public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return Objects.requireNonNull(nodes[i++]);
			}
		};
	}

	/** 仅用于测试/调试：校验堆不变式是否成立。 */
	public boolean isValidQueue() {
		@Nullable T[] nodes = this.nodes;
		int n = nodes.length;
		for (int i = 0; i < n; i++) {
			if (nodes[i] != null) {
				int childLeftIndex = (i << 1) + 1;
				//noinspection DataFlowIssue
				if (childLeftIndex < n && nodes[childLeftIndex] != null &&
						nodes[childLeftIndex].hasHigherPriority(nodes[i]))
					return false;

				int childRightIndex = childLeftIndex + 1;
				//noinspection DataFlowIssue
				if (childRightIndex < n && nodes[childRightIndex] != null &&
						nodes[childRightIndex].hasHigherPriority(nodes[i]))
					return false;
			}
		}
		return true;
	}
}
