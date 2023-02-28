// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

// 有向图的边类型（如：任务的连接方式）
public interface BDAGEdgeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDAGEdge copy();

    Zeze.Builtin.Collections.DAG.BDAGNodeKey getFrom();
    Zeze.Builtin.Collections.DAG.BDAGNodeKey getTo();
}
