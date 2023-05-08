// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BVersionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BVersion copy();

    long getLoginVersion();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    long getReliableNotifyIndex();
    long getReliableNotifyConfirmIndex();
    int getServerId();
    long getLogoutVersion();
    int getState();
}
