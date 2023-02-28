// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BModuleRedirectResult copy();

    int getModuleId();
    int getServerId();
    Zeze.Net.Binary getParams();
}
