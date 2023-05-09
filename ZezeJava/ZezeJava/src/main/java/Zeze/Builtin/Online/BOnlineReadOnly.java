// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BOnlineReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnline copy();

    Zeze.Builtin.Online.BLink getLink();
    long getLoginVersion();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    long getReliableNotifyIndex();
    long getReliableNotifyConfirmIndex();
    int getServerId();
    long getLogoutVersion();
}
