// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BReliableNotifyConfirmReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BReliableNotifyConfirm copy();

    public long getReliableNotifyConfirmIndex();
    public boolean isSync();
}
