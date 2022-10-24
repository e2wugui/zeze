// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BReLoginReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BReLogin copy();

    public String getClientId();
    public long getReliableNotifyConfirmIndex();
}
