// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BReliableNotifyConfirmReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BReliableNotifyConfirm copy();

    public String getClientId();
    public long getReliableNotifyConfirmIndex();
    public boolean isSync();
}
