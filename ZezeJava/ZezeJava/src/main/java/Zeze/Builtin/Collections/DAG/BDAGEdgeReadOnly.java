// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

// 有向图的边类型（如：任务的连接方式）
public interface BDAGEdgeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDAGEdge copy();

    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getFrom();
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getTo();
}
