// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

// 一个节点可以存多个KeyValue对，
public interface BQueueNodeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BQueueNode copy();

    public long getNextNodeId();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeValue, Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly> getValuesReadOnly();
}
