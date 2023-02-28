// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BReliableNotifyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReliableNotify copy();

    Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getNotifiesReadOnly();
    long getReliableNotifyIndex();
}
