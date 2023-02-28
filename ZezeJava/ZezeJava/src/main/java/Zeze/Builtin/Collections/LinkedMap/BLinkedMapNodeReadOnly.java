// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

// 一个节点可以存多个KeyValue对，
public interface BLinkedMapNodeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLinkedMapNode copy();

    long getPrevNodeId();
    long getNextNodeId();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValueReadOnly> getValuesReadOnly();
}
