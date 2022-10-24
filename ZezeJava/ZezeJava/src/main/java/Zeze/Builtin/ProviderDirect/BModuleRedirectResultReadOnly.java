// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BModuleRedirectResult copy();

    public int getModuleId();
    public int getServerId();
    public Zeze.Net.Binary getParams();
}
