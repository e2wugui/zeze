// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectAllResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BModuleRedirectAllResult copy();

    int getModuleId();
    int getServerId();
    long getSourceProvider();
    String getMethodFullName();
    long getSessionId();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> getHashesReadOnly();
}
