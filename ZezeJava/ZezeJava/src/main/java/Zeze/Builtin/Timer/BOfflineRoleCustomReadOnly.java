// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BOfflineRoleCustomReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOfflineRoleCustom copy();

    public String getTimerName();
    public long getRoleId();
    public long getLoginVersion();
    public String getHandleName();
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();

}
