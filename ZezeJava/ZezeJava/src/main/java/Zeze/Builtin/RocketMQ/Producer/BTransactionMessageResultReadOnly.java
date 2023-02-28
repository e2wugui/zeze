// auto-generated @formatter:off
package Zeze.Builtin.RocketMQ.Producer;

public interface BTransactionMessageResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransactionMessageResult copy();

    boolean isResult();
    long getTimestamp();
}
