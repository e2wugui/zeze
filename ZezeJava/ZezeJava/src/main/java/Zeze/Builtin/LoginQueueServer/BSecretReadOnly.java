// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

public interface BSecretReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSecret copy();

    Zeze.Net.Binary getSecretKey();
}
