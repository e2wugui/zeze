// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

public interface BQueueNodeValueReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BQueueNodeValue copy();

    public long getTimestamp();
    public Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly();

}
