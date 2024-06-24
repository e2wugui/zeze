// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 用于BTimer.CustomData,关联角色的offline timer上下文数据
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
