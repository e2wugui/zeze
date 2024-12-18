// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BPushMessageReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPushMessage copy();

    String getTopic();
    long getSessionId();
    Zeze.Builtin.MQ.BMessageReadOnly getMessageReadOnly();
}
