// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BReLoginReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReLogin copy();

    String getClientId();
    long getReliableNotifyConfirmIndex();
}
