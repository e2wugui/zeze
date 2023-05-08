// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// tables
public interface BVersionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BVersion copy();

    long getLoginVersion();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly();
    long getReliableNotifyConfirmIndex();
    long getReliableNotifyIndex();
    int getServerId();
    long getLogoutVersion();
    Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly();
    int getState();
}
