// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// tables
public interface BOnlineReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnline copy();

    Zeze.Builtin.Game.Online.BLink getLink();
    long getLoginVersion();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    long getReliableNotifyConfirmIndex();
    long getReliableNotifyIndex();
    int getServerId();
    long getLogoutVersion();
    Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly();
    String getAccount();
}
