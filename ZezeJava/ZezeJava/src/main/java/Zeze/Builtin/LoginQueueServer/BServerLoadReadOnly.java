// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

public interface BServerLoadReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BServerLoad copy();

    int getServerId();
    String getServiceIp();
    int getServicePort();
    Zeze.Builtin.Provider.BLoadReadOnly getLoadReadOnly();
}
