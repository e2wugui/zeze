// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

// 一个节点可以存多个KeyValue对，
public interface BLinkedMapNodeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLinkedMapNode copy();

    public long getPrevNodeId();
    public long getNextNodeId();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValueReadOnly> getValuesReadOnly();
}
