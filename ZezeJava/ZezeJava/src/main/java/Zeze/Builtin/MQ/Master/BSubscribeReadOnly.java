// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BSubscribeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSubscribe copy();

    String getTopic();
}
