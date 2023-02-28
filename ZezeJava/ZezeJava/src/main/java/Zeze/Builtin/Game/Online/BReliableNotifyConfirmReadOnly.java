// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BReliableNotifyConfirmReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReliableNotifyConfirm copy();

    long getReliableNotifyConfirmIndex();
    boolean isSync();
}
