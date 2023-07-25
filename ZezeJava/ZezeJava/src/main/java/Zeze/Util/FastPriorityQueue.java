package Zeze.Util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * An implementation of a min-Priority Queue using a heap.  Has O(1) .Contains()!
 * <a href="https://github.com/BlueRaja/High-Speed-Priority-Queue-for-C-Sharp/wiki/Getting-Started">for more information</a>
 *
 * @param <T> The values in the queue.  Must extend the FastPriorityQueueNode class
 */
public class FastPriorityQueue<T extends FastPriorityQueueNode<T>> implements Iterable<T> {
	private final int maxCapacity;
	private int numNodes;
	private T[] nodes;

	/**
	 * Instantiate a new Priority Queue
	 *
	 * @param maxCapacity The max nodes ever allowed to be enqueued (going over this will cause undefined behavior)
	 */
	@SuppressWarnings("unchecked")
	public FastPriorityQueue(int initialCapacity, int maxCapacity) {
		if (initialCapacity > maxCapacity)
			throw new IllegalArgumentException("initialCapacity(" + initialCapacity + ") > maxCapacity(" + maxCapacity + ')');
		this.maxCapacity = maxCapacity;
		nodes = (T[])new FastPriorityQueueNode[initialCapacity];
	}

	/**
	 * Returns the number of nodes in the queue.  O(1)
	 */
	public int count() {
		return numNodes;
	}

	/**
	 * Returns the maximum number of items that can be enqueued at once in this queue.
	 * Once you hit this number (ie. once Count == MaxSize),
	 * attempting to enqueue another item will cause undefined behavior.  O(1)
	 */
	public int maxSize() {
		return nodes.length;
	}

	/**
	 * Removes every node from the queue.
	 * O(n) (So, don't do this often!)
	 */
	public void clear() {
		Arrays.fill(nodes, 0, numNodes, null);
		numNodes = 0;
	}

	/**
	 * Returns (in O(1)!) whether the given node is in the queue.  O(1)
	 */
	public boolean contains(T node) {
		int i = node.getQueueIndex();
		return i < numNodes && nodes[i] == node;
	}

	/**
	 * Enqueue a node to the priority queue.  Lower values are placed in front. Ties are broken arbitrarily.
	 * If the queue is full, the result is undefined.
	 * If the node is already enqueued, the result is undefined.
	 * O(log n)
	 */
	public void enqueue(T node) {
		int i = numNodes;
		ensureCapacity(i + 1);
		node.setQueueIndex(i);
		nodes[i] = node;
		numNodes = i + 1;
		cascadeUp(node);
	}

	private void cascadeUp(final T node) {
		// aka Heapify-up
		int i = node.getQueueIndex();
		if (i <= 0)
			return;
		T[] nodes = this.nodes;
		int parent = (i - 1) >> 1;
		T parentNode = nodes[parent];
		if (parentNode.hasHigherOrEqualPriority(node))
			return;

		do {
			// Node has lower priority value, so move parent down the heap to make room
			parentNode.setQueueIndex(i);
			nodes[i] = parentNode;
			i = parent;

			if (parent <= 0)
				break;
			parent = (parent - 1) >> 1;
			parentNode = nodes[parent];
		} while (!parentNode.hasHigherOrEqualPriority(node));

		node.setQueueIndex(i);
		nodes[i] = node;
	}

	private void cascadeDown(T node) {
		// aka Heapify-down
		int finalQueueIndex = node.getQueueIndex();
		int childLeftIndex = (finalQueueIndex << 1) + 1;

		// If leaf node, we're done
		int numNodes = this.numNodes;
		if (childLeftIndex >= numNodes)
			return;

		// Check if the left-child is higher-priority than the current node
		T[] nodes = this.nodes;
		int childRightIndex = childLeftIndex + 1;
		T childLeft = nodes[childLeftIndex];
		if (childLeft.hasHigherPriority(node)) {
			// Check if there is a right child. If not, swap and finish.
			if (childRightIndex >= numNodes) {
				node.setQueueIndex(childLeftIndex);
				childLeft.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childLeft;
				nodes[childLeftIndex] = node;
				return;
			}

			// Check if the left-child is higher-priority than the right-child
			T childRight = nodes[childRightIndex];
			if (childLeft.hasHigherPriority(childRight)) { // left is highest, move it up and continue
				childLeft.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childLeft;
				finalQueueIndex = childLeftIndex;
			} else { // right is even higher, move it up and continue
				childRight.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childRight;
				finalQueueIndex = childRightIndex;
			}
		} else if (childRightIndex >= numNodes) // Not swapping with left-child, does right-child exist?
			return;
		else {
			// Check if the right-child is higher-priority than the current node
			T childRight = nodes[childRightIndex];
			if (childRight.hasHigherPriority(node)) {
				childRight.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = childRight;
				finalQueueIndex = childRightIndex;
			} else // Neither child is higher-priority than current, so finish and stop.
				return;
		}

		for (; ; ) {
			childLeftIndex = (finalQueueIndex << 1) + 1;

			// If leaf node, we're done
			if (childLeftIndex >= numNodes) {
				node.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = node;
				break;
			}

			// Check if the left-child is higher-priority than the current node
			childRightIndex = childLeftIndex + 1;
			childLeft = nodes[childLeftIndex];
			if (childLeft.hasHigherPriority(node)) {
				// Check if there is a right child. If not, swap and finish.
				if (childRightIndex >= numNodes) {
					node.setQueueIndex(childLeftIndex);
					childLeft.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childLeft;
					nodes[childLeftIndex] = node;
					break;
				}

				// Check if the left-child is higher-priority than the right-child
				T childRight = nodes[childRightIndex];
				if (childLeft.hasHigherPriority(childRight)) { // left is highest, move it up and continue
					childLeft.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childLeft;
					finalQueueIndex = childLeftIndex;
				} else { // right is even higher, move it up and continue
					childRight.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childRight;
					finalQueueIndex = childRightIndex;
				}
			} else if (childRightIndex >= numNodes) { // Not swapping with left-child, does right-child exist?
				node.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = node;
				break;
			} else { // Check if the right-child is higher-priority than the current node
				T childRight = nodes[childRightIndex];
				if (childRight.hasHigherPriority(node)) {
					childRight.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = childRight;
					finalQueueIndex = childRightIndex;
				} else { // Neither child is higher-priority than current, so finish and stop.
					node.setQueueIndex(finalQueueIndex);
					nodes[finalQueueIndex] = node;
					break;
				}
			}
		}
	}

	/**
	 * Removes the head of the queue and returns it.
	 * If queue is empty, result is undefined
	 * O(log n)
	 */
	public T dequeue() {
		T[] nodes = this.nodes;
		T returnMe = nodes[0];
		int numNodes = this.numNodes;
		this.numNodes = --numNodes;
		if (numNodes == 0) // If the node is already the last node, we can remove it immediately
			nodes[0] = null;
		else {
			// Swap the node with the last node
			T formerLastNode = nodes[numNodes];
			formerLastNode.setQueueIndex(0);
			nodes[0] = formerLastNode;
			nodes[numNodes] = null;

			// Now bubble formerLastNode (which is no longer the last node) down
			cascadeDown(formerLastNode);
		}
		return returnMe;
	}

	/**
	 * Resize the queue so it can accept more nodes.  All currently enqueued nodes are remain.
	 * Attempting to decrease the queue size to a size too small to hold the existing nodes results in undefined behavior
	 * O(n)
	 */
	public void resize(int maxNodes) {
		nodes = Arrays.copyOf(nodes, maxNodes);
	}

	public void ensureCapacity(int capacity) {
		int n = nodes.length;
		if (n < capacity) {
			if (capacity > maxCapacity)
				throw new IllegalArgumentException("capacity(" + capacity + ") > maxCapacity(" + maxCapacity + ')');
			resize((int)Math.min(Math.max((long)n << 1, capacity), maxCapacity));
		}
	}

	/**
	 * Returns the head of the queue, without removing it (use Dequeue() for that).
	 * If the queue is empty, behavior is undefined.
	 * O(1)
	 */
	public T first() {
		return nodes[0];
	}

	/**
	 * This method must be called on a node every time its priority changes while it is in the queue.
	 * <b>Forgetting to call this method will result in a corrupted queue!</b>
	 * Calling this method on a node not in the queue results in undefined behavior
	 * O(log n)
	 */
	public void updatePriority(T node) {
		onNodeUpdated(node);
	}

	private void onNodeUpdated(T node) {
		// Bubble the updated node up or down as appropriate
		int parentIndex = (node.getQueueIndex() - 1) >> 1;
		if (parentIndex >= 0 && node.hasHigherPriority(nodes[parentIndex]))
			cascadeUp(node);
		else // Note that CascadeDown will be called if parentNode == node (that is, node is the root)
			cascadeDown(node);
	}

	/**
	 * Removes a node from the queue.  The node does not need to be the head of the queue.
	 * If the node is not in the queue, the result is undefined.  If unsure, check Contains() first
	 * O(log n)
	 */
	public void remove(final T node) {
		T[] nodes = this.nodes;
		int numNodes = this.numNodes;
		this.numNodes = --numNodes;
		int i = node.getQueueIndex();
		if (i == numNodes) // If the node is already the last node, we can remove it immediately
			nodes[numNodes] = null;
		else {
			// Swap the node with the last node
			T formerLastNode = nodes[numNodes];
			nodes[i] = formerLastNode;
			formerLastNode.setQueueIndex(i);
			nodes[numNodes] = null;

			// Now bubble formerLastNode (which is no longer the last node) up or down as appropriate
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
				return nodes[i++];
			}
		};
	}

	/**
	 * <b>Should not be called in production code.</b>
	 * Checks to make sure the queue is still in a valid state.  Used for testing/debugging the queue.
	 */
	public boolean isValidQueue() {
		T[] nodes = this.nodes;
		int n = nodes.length;
		for (int i = 0; i < n; i++) {
			if (nodes[i] != null) {
				int childLeftIndex = (i << 1) + 1;
				if (childLeftIndex < n && nodes[childLeftIndex] != null &&
						nodes[childLeftIndex].hasHigherPriority(nodes[i]))
					return false;

				int childRightIndex = childLeftIndex + 1;
				if (childRightIndex < n && nodes[childRightIndex] != null &&
						nodes[childRightIndex].hasHigherPriority(nodes[i]))
					return false;
			}
		}
		return true;
	}
}
