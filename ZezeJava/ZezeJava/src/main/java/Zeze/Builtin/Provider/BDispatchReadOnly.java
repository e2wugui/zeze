// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// link to gs
public interface BDispatchReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BDispatch copy();
    BDispatch.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getLinkSid();
    String getAccount();
    long getProtocolType();
    Zeze.Net.Binary getProtocolData();
    String getContext();
    Zeze.Net.Binary getContextx();
    String getOnlineSetName();
}
