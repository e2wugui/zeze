// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

public interface BQueuePositionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueuePosition copy();

    int getQueuePosition();
}
