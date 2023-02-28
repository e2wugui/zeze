// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BProviderInfoReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BProviderInfo copy();

    String getIp();
    int getPort();
    int getServerId();
}
