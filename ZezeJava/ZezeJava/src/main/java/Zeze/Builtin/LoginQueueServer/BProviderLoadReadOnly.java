// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

public interface BProviderLoadReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BProviderLoad copy();

    int getServerId();
    String getServiceIp();
    int getServicePort();
    Zeze.Builtin.Provider.BLoadReadOnly getLoadReadOnly();
}
