// auto-generated @formatter:off
package Zeze.Builtin.RocketMQ.Producer;

public interface BTransactionMessageResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTransactionMessageResult copy();

    public boolean isResult();
    public long getTimestamp();
}
