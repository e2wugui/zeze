// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BMessageReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMessage copy();

    String getMessageId();
    Zeze.Net.Binary getBody();
}
