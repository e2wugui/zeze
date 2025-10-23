// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BOnlinesReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BOnlines copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BOnline, Zeze.Builtin.Online.BOnlineReadOnly> getLoginsReadOnly();
    long getLastLoginVersion();
    String getAccount();
}
