// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

public interface BQueueSizeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueueSize copy();

    int getQueueSize();
}
