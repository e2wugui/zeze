// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BOfflineRoleCustomReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOfflineRoleCustom copy();

    String getTimerName();
    long getRoleId();
    long getLoginVersion();
    String getHandleName();
    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
    String getOnlineSetName();
}
