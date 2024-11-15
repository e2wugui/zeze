// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BOpenMQReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOpenMQ copy();

    String getName();
    Zeze.Builtin.MQ.BOptionsReadOnly getOptionsReadOnly();
}
