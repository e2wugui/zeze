// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BPartitionSubscribeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPartitionSubscribe copy();

    String getTopic();
    Zeze.Transaction.Collections.PList1ReadOnly<Integer> getPartitionIndexReadOnly();
}
