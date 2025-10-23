// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 若干个timer打包的节点
public interface BNodeReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BNode copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getPrevNodeId();
    long getNextNodeId();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BTimer, Zeze.Builtin.Timer.BTimerReadOnly> getTimersReadOnly();
}
