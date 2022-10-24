// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectAllResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BModuleRedirectAllResult copy();

    public int getModuleId();
    public int getServerId();
    public long getSourceProvider();
    public String getMethodFullName();
    public long getSessionId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash, Zeze.Builtin.ProviderDirect.BModuleRedirectAllHashReadOnly> getHashsReadOnly();
}
