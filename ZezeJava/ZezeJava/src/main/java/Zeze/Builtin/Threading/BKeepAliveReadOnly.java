// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BKeepAliveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BKeepAlive copy();

    int getServerId();
    long getAppSerialId();
}
