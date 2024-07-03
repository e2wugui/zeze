// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BOnlineReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnline copy();

    int getServerId();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    long getReliableNotifyConfirmIndex();
    long getReliableNotifyIndex();
    Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly();
}
