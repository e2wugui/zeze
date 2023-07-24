package Zeze.Util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * An implementation of a min-Priority Queue using a heap.  Has O(1) .Contains()!
 * <a href="https://github.com/BlueRaja/High-Speed-Priority-Queue-for-C-Sharp/wiki/Getting-Started">for more information</a>
 *
 * @param <T> The values in the queue.  Must extend the FastPriorityQueueNode class
 */
public class FastPriorityQueue<T extends FastPriorityQueueNode<T>> {
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
		this.maxCapacity = maxCapacity;
		nodes = (T[])new FastPriorityQueueNode[initialCapacity + 1];
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
		return nodes.length - 1;
	}

	/**
	 * Removes every node from the queue.
	 * O(n) (So, don't do this often!)
	 */
	public void clear() {
		Arrays.fill(nodes, 0, numNodes + 1, null);
		numNodes = 0;
	}

	/**
	 * Returns (in O(1)!) whether the given node is in the queue.  O(1)
	 */
	public boolean contains(T node) {
		int i = node.getQueueIndex();
		return i < nodes.length && nodes[i] == node;
	}

	/**
	 * Enqueue a node to the priority queue.  Lower values are placed in front. Ties are broken arbitrarily.
	 * If the queue is full, the result is undefined.
	 * If the node is already enqueued, the result is undefined.
	 * O(log n)
	 */
	public void enqueue(T node) {
		ensureCapacity();
		nodes[++numNodes] = node;
		node.setQueueIndex(numNodes);
		cascadeUp(node);
	}

	private void cascadeUp(final T node) {
		// aka Heapify-up
		int i = node.getQueueIndex();
		if (i <= 1)
			return;
		int parent = i >> 1;
		T parentNode = nodes[parent];
		if (parentNode.hasHigherOrEqualPriority(node))
			return;

		// Node has lower priority value, so move parent down the heap to make room
		nodes[i] = parentNode;
		parentNode.setQueueIndex(i);

		node.setQueueIndex(parent);

		while (parent > 1) {
			parent >>= 1;
			parentNode = nodes[parent];
			if (parentNode.hasHigherOrEqualPriority(node))
				break;

			// Node has lower priority value, so move parent down the heap to make room
			nodes[i] = parentNode;
			parentNode.setQueueIndex(i);

			node.setQueueIndex(parent);
		}

		nodes[i] = node;
	}

	private void cascadeDown(T node) {
		// aka Heapify-down
		int finalQueueIndex = node.getQueueIndex();
		int childLeftIndex = 2 * finalQueueIndex;

		// If leaf node, we're done
		if (childLeftIndex > numNodes)
			return;

		// Check if the left-child is higher-priority than the current node
		int childRightIndex = childLeftIndex + 1;
		T childLeft = nodes[childLeftIndex];
		if (childLeft.hasHigherPriority(node)) {
			// Check if there is a right child. If not, swap and finish.
			if (childRightIndex > numNodes) {
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
		} else if (childRightIndex > numNodes) // Not swapping with left-child, does right-child exist?
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
			childLeftIndex = 2 * finalQueueIndex;

			// If leaf node, we're done
			if (childLeftIndex > numNodes) {
				node.setQueueIndex(finalQueueIndex);
				nodes[finalQueueIndex] = node;
				break;
			}

			// Check if the left-child is higher-priority than the current node
			childRightIndex = childLeftIndex + 1;
			childLeft = nodes[childLeftIndex];
			if (childLeft.hasHigherPriority(node)) {
				// Check if there is a right child. If not, swap and finish.
				if (childRightIndex > numNodes) {
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
			} else if (childRightIndex > numNodes) { // Not swapping with left-child, does right-child exist?
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
		T returnMe = nodes[1];
		if (numNodes == 1) { // If the node is already the last node, we can remove it immediately
			nodes[1] = null;
			numNodes = 0;
			return returnMe;
		}

		// Swap the node with the last node
		T formerLastNode = nodes[numNodes];
		nodes[1] = formerLastNode;
		formerLastNode.setQueueIndex(1);
		nodes[numNodes--] = null;

		// Now bubble formerLastNode (which is no longer the last node) down
		cascadeDown(formerLastNode);
		return returnMe;
	}

	/**
	 * Resize the queue so it can accept more nodes.  All currently enqueued nodes are remain.
	 * Attempting to decrease the queue size to a size too small to hold the existing nodes results in undefined behavior
	 * O(n)
	 */
	public void resize(int maxNodes) {
		nodes = Arrays.copyOf(nodes, maxNodes + 1);
	}

	public void ensureCapacity() {
		if (nodes.length - numNodes <= 2)
			resize(Math.min(nodes.length * 2, maxCapacity));
	}

	/**
	 * Returns the head of the queue, without removing it (use Dequeue() for that).
	 * If the queue is empty, behavior is undefined.
	 * O(1)
	 */
	public T first() {
		return nodes[1];
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
		int parentIndex = node.getQueueIndex() >> 1;

		if (parentIndex > 0 && node.hasHigherPriority(nodes[parentIndex]))
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
		int i = node.getQueueIndex();
		if (i == numNodes) { // If the node is already the last node, we can remove it immediately
			nodes[numNodes--] = null;
			return;
		}

		// Swap the node with the last node
		T formerLastNode = nodes[numNodes];
		nodes[i] = formerLastNode;
		formerLastNode.setQueueIndex(i);
		nodes[numNodes--] = null;

		// Now bubble formerLastNode (which is no longer the last node) up or down as appropriate
		onNodeUpdated(formerLastNode);
	}

	public Iterator<T> GetEnumerator() {
		return new Iterator<>() {
			private int i = 1;

			@Override
			public boolean hasNext() {
				return i <= numNodes;
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
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i] != null) {
				int childLeftIndex = 2 * i;
				if (childLeftIndex < nodes.length && nodes[childLeftIndex] != null &&
						nodes[childLeftIndex].hasHigherPriority(nodes[i]))
					return false;

				int childRightIndex = childLeftIndex + 1;
				if (childRightIndex < nodes.length && nodes[childRightIndex] != null &&
						nodes[childRightIndex].hasHigherPriority(nodes[i]))
					return false;
			}
		}
		return true;
	}
}
