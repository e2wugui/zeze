// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// Offline Timer
public interface BOfflineAccountCustomReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOfflineAccountCustom copy();

    public String getTimerName();
    public String getAccount();
    public String getClientId();
    public long getLoginVersion();
    public String getHandleName();
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();

}
