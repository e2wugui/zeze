package Zeze.Util;

/** {@link FastPriorityQueue} 的节点接口：维护队列内下标与优先级比较。 */
public interface FastPriorityQueueNode<T> {
	/** 返回当前节点在队列中的下标。 */
	int getQueueIndex();

	void setQueueIndex(int index);

	/** 返回当前节点是否比 {@code lower} 优先级更高（同节点比较应返回 false）。 */
	boolean hasHigherPriority(T lower);
}
