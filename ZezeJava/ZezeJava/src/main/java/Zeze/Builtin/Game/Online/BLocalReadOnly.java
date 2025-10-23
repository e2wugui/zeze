// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BLocalReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BLocal copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getLoginVersion();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Online.BAny, Zeze.Builtin.Game.Online.BAnyReadOnly> getDatasReadOnly();
    Zeze.Builtin.Game.Online.BLink getLink();
}
