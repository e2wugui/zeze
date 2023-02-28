// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

public interface BQueueNodeValueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueueNodeValue copy();

    long getTimestamp();
    Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly();
}
