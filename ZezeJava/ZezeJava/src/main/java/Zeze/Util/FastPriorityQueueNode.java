package Zeze.Util;

public interface FastPriorityQueueNode<T>
{
    /// <summary>
    /// Represents the current position in the queue
    /// </summary>
    int getQueueIndex();
    void setQueueIndex(int value);

    /// <summary>
    /// Returns true if 'higher' has higher priority than 'lower', false otherwise.
    /// Note that calling HasHigherPriority(node, node) (ie. both arguments the same node) will return false
    /// </summary>
    boolean HasHigherPriority(T lower);

    /// <summary>
    /// Returns true if 'higher' has higher priority than 'lower', false otherwise.
    /// Note that calling HasHigherOrEqualPriority(node, node) (ie. both arguments the same node) will return true
    /// </summary>
    boolean HasHigherOrEqualPriority(T lower);
}