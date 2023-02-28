// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// Offline Timer
public interface BOfflineAccountCustomReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOfflineAccountCustom copy();

    String getTimerName();
    String getAccount();
    String getClientId();
    long getLoginVersion();
    String getHandleName();
    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
}
