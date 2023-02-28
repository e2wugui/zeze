// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BReLoginReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReLogin copy();

    long getRoleId();
    long getReliableNotifyConfirmIndex();
}
