// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// tables
public interface BVersionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BVersion copy();

    public long getLoginVersion();
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    public long getReliableNotifyConfirmIndex();
    public long getReliableNotifyIndex();
    public int getServerId();
    public long getLogoutVersion();
    public Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly();

}
