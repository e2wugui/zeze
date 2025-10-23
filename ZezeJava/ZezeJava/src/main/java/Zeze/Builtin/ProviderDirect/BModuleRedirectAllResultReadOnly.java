// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectAllResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BModuleRedirectAllResult copy();
    BModuleRedirectAllResult.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getModuleId();
    int getServerId();
    long getSourceProvider();
    String getMethodFullName();
    long getSessionId();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> getHashesReadOnly();
}
