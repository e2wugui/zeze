package Zeze.Util;

import java.util.Arrays;

/// <summary>
/// An implementation of a min-Priority Queue using a heap.  Has O(1) .Contains()!
/// See https://github.com/BlueRaja/High-Speed-Priority-Queue-for-C-Sharp/wiki/Getting-Started for more information
/// </summary>
/// <typeparam name="T">The values in the queue.  Must extend the FastPriorityQueueNode class</typeparam>
public class FastPriorityQueue<T extends FastPriorityQueueNode<T>> {
    private int _maxCapacity;
    private int _numNodes;
    private Object[] _nodes;

    /// <summary>
/// Instantiate a new Priority Queue
/// </summary>
/// <param name="maxCapacity">The max nodes ever allowed to be enqueued (going over this will cause undefined behavior)</param>
    public FastPriorityQueue(int initalCapacity, int maxCapacity) {
        _maxCapacity = maxCapacity;
        _numNodes = 0;
        _nodes = new Object[initalCapacity + 1];
    }

    /// <summary>
/// Returns the number of nodes in the queue.
/// O(1)
/// </summary>
    public int Count(){
        return _numNodes;
    }

    /// <summary>
/// Returns the maximum number of items that can be enqueued at once in this queue.  Once you hit this number (ie. once Count == MaxSize),
/// attempting to enqueue another item will cause undefined behavior.  O(1)
/// </summary>
    public int MaxSize(){
        return _nodes.length - 1;
    }


    /// <summary>
    /// Removes every node from the queue.
    /// O(n) (So, don't do this often!)
    /// </summary>
    public void Clear() {
        Arrays.fill(_nodes, null);
        _numNodes = 0;
    }

    /// <summary>
    /// Returns (in O(1)!) whether the given node is in the queue.  O(1)
    /// </summary>
    public boolean Contains(T node) {
        return (node.getQueueIndex() < _nodes.length && _nodes[node.getQueueIndex()] == node);
    }

    /// <summary>
    /// Enqueue a node to the priority queue.  Lower values are placed in front. Ties are broken arbitrarily.
    /// If the queue is full, the result is undefined.
    /// If the node is already enqueued, the result is undefined.
    /// O(log n)
    /// </summary>
    public void Enqueue(T node) {
        EnsureCapacity();
        _numNodes++;
        _nodes[_numNodes] = node;
        node.setQueueIndex(_numNodes);
        CascadeUp(node);
    }


    private void CascadeUp(T node) {
        //aka Heapify-up
        int parent;
        if (node.getQueueIndex() > 1) {
            parent = node.getQueueIndex() >> 1;
            T parentNode = (T)_nodes[parent];
            if (parentNode.HasHigherOrEqualPriority(node))
                return;

            //Node has lower priority value, so move parent down the heap to make room
            _nodes[node.getQueueIndex()] = parentNode;
            parentNode.setQueueIndex(node.getQueueIndex());

            node.setQueueIndex(parent);
        } else {
            return;
        }

        while (parent > 1) {
            parent >>= 1;
            T parentNode = (T)_nodes[parent];
            if (parentNode.HasHigherOrEqualPriority(node))
                break;

            //Node has lower priority value, so move parent down the heap to make room
            _nodes[node.getQueueIndex()] = parentNode;
            parentNode.setQueueIndex(node.getQueueIndex());

            node.setQueueIndex(parent);
        }

        _nodes[node.getQueueIndex()] = node;
    }


    private void CascadeDown(T node) {
        //aka Heapify-down
        int finalQueueIndex = node.getQueueIndex();
        int childLeftIndex = 2 * finalQueueIndex;

        // If leaf node, we're done
        if (childLeftIndex > _numNodes) {
            return;
        }

        // Check if the left-child is higher-priority than the current node
        int childRightIndex = childLeftIndex + 1;
        T childLeft = (T)_nodes[childLeftIndex];
        if (childLeft.HasHigherPriority(node)) {
            // Check if there is a right child. If not, swap and finish.
            if (childRightIndex > _numNodes) {
                node.setQueueIndex(childLeftIndex);
                childLeft.setQueueIndex(finalQueueIndex);
                _nodes[finalQueueIndex] = childLeft;
                _nodes[childLeftIndex] = node;
                return;
            }

            // Check if the left-child is higher-priority than the right-child
            T childRight = (T)_nodes[childRightIndex];
            if (childLeft.HasHigherPriority(childRight)) {
                // left is highest, move it up and continue
                childLeft.setQueueIndex(finalQueueIndex);
                _nodes[finalQueueIndex] = childLeft;
                finalQueueIndex = childLeftIndex;
            } else {
                // right is even higher, move it up and continue
                childRight.setQueueIndex(finalQueueIndex);
                _nodes[finalQueueIndex] = childRight;
                finalQueueIndex = childRightIndex;
            }
        }
        // Not swapping with left-child, does right-child exist?
        else if (childRightIndex > _numNodes) {
            return;
        } else {
            // Check if the right-child is higher-priority than the current node
            T childRight = (T)_nodes[childRightIndex];
            if (childRight.HasHigherPriority(node)) {
                childRight.setQueueIndex(finalQueueIndex);
                _nodes[finalQueueIndex] = childRight;
                finalQueueIndex = childRightIndex;
            }
            // Neither child is higher-priority than current, so finish and stop.
            else {
                return;
            }
        }

        while (true) {
            childLeftIndex = 2 * finalQueueIndex;

            // If leaf node, we're done
            if (childLeftIndex > _numNodes) {
                node.setQueueIndex(finalQueueIndex);
                _nodes[finalQueueIndex] = node;
                break;
            }

            // Check if the left-child is higher-priority than the current node
            childRightIndex = childLeftIndex + 1;
            childLeft = (T)_nodes[childLeftIndex];
            if (childLeft.HasHigherPriority(node)) {
                // Check if there is a right child. If not, swap and finish.
                if (childRightIndex > _numNodes) {
                    node.setQueueIndex(childLeftIndex);
                    childLeft.setQueueIndex(finalQueueIndex);
                    _nodes[finalQueueIndex] = childLeft;
                    _nodes[childLeftIndex] = node;
                    break;
                }

                // Check if the left-child is higher-priority than the right-child
                T childRight = (T)_nodes[childRightIndex];
                if (childLeft.HasHigherPriority(childRight)) {
                    // left is highest, move it up and continue
                    childLeft.setQueueIndex(finalQueueIndex);
                    _nodes[finalQueueIndex] = childLeft;
                    finalQueueIndex = childLeftIndex;
                } else {
                    // right is even higher, move it up and continue
                    childRight.setQueueIndex(finalQueueIndex);
                    _nodes[finalQueueIndex] = childRight;
                    finalQueueIndex = childRightIndex;
                }
            }
            // Not swapping with left-child, does right-child exist?
            else if (childRightIndex > _numNodes) {
                node.setQueueIndex(finalQueueIndex);
                _nodes[finalQueueIndex] = node;
                break;
            } else {
                // Check if the right-child is higher-priority than the current node
                T childRight = (T)_nodes[childRightIndex];
                if (childRight.HasHigherPriority(node)) {
                    childRight.setQueueIndex(finalQueueIndex);
                    _nodes[finalQueueIndex] = childRight;
                    finalQueueIndex = childRightIndex;
                }
                // Neither child is higher-priority than current, so finish and stop.
                else {
                    node.setQueueIndex(finalQueueIndex);
                    _nodes[finalQueueIndex] = node;
                    break;
                }
            }
        }
    }

    /// <summary>
    /// Removes the head of the queue and returns it.
    /// If queue is empty, result is undefined
    /// O(log n)
    /// </summary>

    public T Dequeue() {
        T returnMe = (T)_nodes[1];
        //If the node is already the last node, we can remove it immediately
        if (_numNodes == 1) {
            _nodes[1] = null;
            _numNodes = 0;
            return returnMe;
        }

        //Swap the node with the last node
        T formerLastNode = (T)_nodes[_numNodes];
        _nodes[1] = formerLastNode;
        formerLastNode.setQueueIndex(1);
        _nodes[_numNodes] = null;
        _numNodes--;

        //Now bubble formerLastNode (which is no longer the last node) down
        CascadeDown(formerLastNode);
        return returnMe;
    }

    /// <summary>
/// Resize the queue so it can accept more nodes.  All currently enqueued nodes are remain.
/// Attempting to decrease the queue size to a size too small to hold the existing nodes results in undefined behavior
/// O(n)
/// </summary>
    public void Resize(int maxNodes) {
        _nodes = Arrays.copyOf(_nodes, maxNodes + 1);
    }

    public void EnsureCapacity() {
        if (_nodes.length - _numNodes <= 2) {
            int newCapacity = Math.min(_nodes.length * 2, _maxCapacity);
            Resize(newCapacity);
        }
    }

    /// <summary>
/// Returns the head of the queue, without removing it (use Dequeue() for that).
/// If the queue is empty, behavior is undefined.
/// O(1)
/// </summary>
    public T First()

    {
        return (T)_nodes[1];
    }

    /// <summary>
    /// This method must be called on a node every time its priority changes while it is in the queue.
    /// <b>Forgetting to call this method will result in a corrupted queue!</b>
    /// Calling this method on a node not in the queue results in undefined behavior
    /// O(log n)
    /// </summary>

    public void UpdatePriority(T node) {
        OnNodeUpdated(node);
    }


    private void OnNodeUpdated(T node) {
        //Bubble the updated node up or down as appropriate
        int parentIndex = node.getQueueIndex() >> 1;

        if (parentIndex > 0 && node.HasHigherPriority((T)_nodes[parentIndex])) {
            CascadeUp(node);
        } else {
            //Note that CascadeDown will be called if parentNode == node (that is, node is the root)
            CascadeDown(node);
        }
    }

    /// <summary>
    /// Removes a node from the queue.  The node does not need to be the head of the queue.
    /// If the node is not in the queue, the result is undefined.  If unsure, check Contains() first
    /// O(log n)
    /// </summary>

    public void Remove(T node) {
        //If the node is already the last node, we can remove it immediately
        if (node.getQueueIndex() == _numNodes) {
            _nodes[_numNodes] = null;
            _numNodes--;
            return;
        }

        //Swap the node with the last node
        T formerLastNode = (T)_nodes[_numNodes];
        _nodes[node.getQueueIndex()] = formerLastNode;
        formerLastNode.setQueueIndex(node.getQueueIndex());
        _nodes[_numNodes] = null;
        _numNodes--;

        //Now bubble formerLastNode (which is no longer the last node) up or down as appropriate
        OnNodeUpdated(formerLastNode);
    }

//    public IEnumerator<T> GetEnumerator() {
//        #if NET_VERSION_4_5 // ArraySegment does not implement IEnumerable before 4.5
//        IEnumerable<T> e = new ArraySegment<T>(_nodes, 1, _numNodes);
//        return e.GetEnumerator();
//        #else
//        for (int i = 1; i <= _numNodes; i++)
//            yield return _nodes[i];
//        #endif
//    }

    /// <summary>
/// <b>Should not be called in production code.</b>
/// Checks to make sure the queue is still in a valid state.  Used for testing/debugging the queue.
/// </summary>
    public boolean IsValidQueue() {
        for (int i = 1; i < _nodes.length; i++) {
            if (_nodes[i] != null) {
                int childLeftIndex = 2 * i;
                if (childLeftIndex < _nodes.length && _nodes[childLeftIndex] != null &&
                        ((T)_nodes[childLeftIndex]).HasHigherPriority((T)_nodes[i]))
                    return false;

                int childRightIndex = childLeftIndex + 1;
                if (childRightIndex < _nodes.length && _nodes[childRightIndex] != null &&
                        ((T)_nodes[childRightIndex]).HasHigherPriority((T)_nodes[i]))
                    return false;
            }
        }

        return true;
    }
}
