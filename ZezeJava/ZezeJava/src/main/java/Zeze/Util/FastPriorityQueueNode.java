package Zeze.Util;

public interface FastPriorityQueueNode<T> {
	/**
	 * Represents the current position in the queue
	 */
	int getQueueIndex();

	void setQueueIndex(int index);

	/**
	 * Returns true if 'this' has higher priority than 'lower', false otherwise.
	 * Note that calling node.hasHigherPriority(node) (ie. both arguments the same node) will return false
	 */
	boolean hasHigherPriority(T lower);
}
