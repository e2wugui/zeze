// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectAllRequestReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BModuleRedirectAllRequest copy();

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
