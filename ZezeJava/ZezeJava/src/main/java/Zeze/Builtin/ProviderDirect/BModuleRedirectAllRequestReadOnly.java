// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectAllRequestReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BModuleRedirectAllRequest copy();

    public int getModuleId();
    public int getHashCodeConcurrentLevel();
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getHashCodesReadOnly();
    public long getSourceProvider();
    public long getSessionId();
    public String getMethodFullName();
    public Zeze.Net.Binary getParams();
    public String getServiceNamePrefix();
}
