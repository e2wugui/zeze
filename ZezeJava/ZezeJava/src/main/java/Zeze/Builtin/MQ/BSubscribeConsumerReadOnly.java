// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BSubscribeConsumerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSubscribeConsumer copy();

    String getTopic();
    long getSessionId();
}
