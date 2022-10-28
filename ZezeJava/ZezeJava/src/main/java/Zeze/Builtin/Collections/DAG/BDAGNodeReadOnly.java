// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

// 有向图的结点类型（如：一个任务Task）
public interface BDAGNodeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDAGNode copy();

    public Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly();

}
