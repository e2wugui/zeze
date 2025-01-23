// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BCreatePartitionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCreatePartition copy();

    String getTopic();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getPartitionIndexesReadOnly();
}
