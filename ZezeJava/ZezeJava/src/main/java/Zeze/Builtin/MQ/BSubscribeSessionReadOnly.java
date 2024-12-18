// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BSubscribeSessionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSubscribeSession copy();

    String getTopic();
    long getSessionId();
}
