// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

// 一个节点可以存多个KeyValue对，
public interface BQueueNodeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueueNode copy();

    long getNextNodeId();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeValue, Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly> getValuesReadOnly();
    Zeze.Builtin.Collections.Queue.BQueueNodeKey getNextNodeKey();
}
