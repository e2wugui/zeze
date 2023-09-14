// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BModuleRedirectArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BModuleRedirectArgument copy();

    int getModuleId();
    int getHashCode();
    int getRedirectType();
    String getMethodFullName();
    Zeze.Net.Binary getParams();
    String getServiceNamePrefix();
    int getVersion();
    int getKey();
    boolean isNoOneByOne();
}
