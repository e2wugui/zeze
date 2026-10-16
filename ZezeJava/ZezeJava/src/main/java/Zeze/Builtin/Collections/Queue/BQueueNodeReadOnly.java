// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 一个节点可以存多个KeyValue对，
public interface BQueueNodeReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BQueueNode copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getNextNodeId();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeValue, Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly> getValuesReadOnly();
    Zeze.Builtin.Collections.Queue.BQueueNodeKey getNextNodeKey();
}
