// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BTransmitReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BTransmit copy();
    BTransmit.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getActionName();
    Zeze.Transaction.Collections.PSet1ReadOnly<Long> getRolesReadOnly();
    long getSender();
    Zeze.Net.Binary getParameter();
    String getOnlineSetName();
}
