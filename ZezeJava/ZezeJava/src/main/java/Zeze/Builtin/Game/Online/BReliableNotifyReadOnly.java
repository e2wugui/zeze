// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BReliableNotifyReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BReliableNotify copy();

    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getNotifiesReadOnly();
    public long getReliableNotifyIndex();
}
