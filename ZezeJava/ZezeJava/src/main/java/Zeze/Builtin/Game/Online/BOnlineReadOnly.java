// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BOnlineReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BOnline copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getServerId();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    long getReliableNotifyConfirmIndex();
    long getReliableNotifyIndex();
    Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly();
}
