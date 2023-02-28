// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BReliableNotifyConfirmReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReliableNotifyConfirm copy();

    String getClientId();
    long getReliableNotifyConfirmIndex();
    boolean isSync();
}
