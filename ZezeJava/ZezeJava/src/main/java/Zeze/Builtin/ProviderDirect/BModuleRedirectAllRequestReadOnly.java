// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectAllRequestReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BModuleRedirectAllRequest copy();
    BModuleRedirectAllRequest.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getModuleId();
    int getHashCodeConcurrentLevel();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getHashCodesReadOnly();
    long getSourceProvider();
    long getSessionId();
    String getMethodFullName();
    Zeze.Net.Binary getParams();
    String getServiceNamePrefix();
    int getVersion();
}
