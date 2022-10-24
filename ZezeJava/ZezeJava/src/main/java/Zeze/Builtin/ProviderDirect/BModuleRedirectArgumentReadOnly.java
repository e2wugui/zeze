// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectArgumentReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BModuleRedirectArgument copy();

    public int getModuleId();
    public int getHashCode();
    public int getRedirectType();
    public String getMethodFullName();
    public Zeze.Net.Binary getParams();
    public String getServiceNamePrefix();
}
