// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

// 有向图的边类型（如：任务的连接方式）
public interface BDAGEdgeReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BDAGEdge copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Builtin.Collections.DAG.BDAGNodeKey getFrom();
    Zeze.Builtin.Collections.DAG.BDAGNodeKey getTo();
}
