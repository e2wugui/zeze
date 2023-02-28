// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

// 有向图的结点类型（如：一个任务Task）
public interface BDAGNodeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDAGNode copy();

    Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly();
}
