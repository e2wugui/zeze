// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// tables
public interface BOnlineSharedReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnlineShared copy();

    String getAccount();
    Zeze.Builtin.Game.Online.BLink getLink();
    long getLoginVersion();
    long getLogoutVersion();
    Zeze.Transaction.DynamicBeanReadOnly getUserDataReadOnly();
}
