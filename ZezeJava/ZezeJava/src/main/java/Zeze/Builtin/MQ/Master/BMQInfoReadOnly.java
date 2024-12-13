// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BMQInfoReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMQInfo copy();

    String getTopic();
    int getPartition();
    Zeze.Builtin.MQ.BOptionsReadOnly getOptionsReadOnly();
}
