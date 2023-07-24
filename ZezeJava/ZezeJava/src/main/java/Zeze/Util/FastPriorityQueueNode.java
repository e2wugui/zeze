package Zeze.Util;

public interface FastPriorityQueueNode<T> {
	/**
	 * Represents the current position in the queue
	 */
	int getQueueIndex();

	void setQueueIndex(int value);

	/**
	 * Returns true if 'higher' has higher priority than 'lower', false otherwise.
	 * Note that calling hasHigherPriority(node, node) (ie. both arguments the same node) will return false
	 */
	boolean hasHigherPriority(T lower);

	/**
	 * Returns true if 'higher' has higher priority than 'lower', false otherwise.
	 * Note that calling hasHigherOrEqualPriority(node, node) (ie. both arguments the same node) will return true
	 */
	boolean hasHigherOrEqualPriority(T lower);
}
