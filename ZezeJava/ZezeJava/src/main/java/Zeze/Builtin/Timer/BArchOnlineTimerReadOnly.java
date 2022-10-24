// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 保存在内存Map中
public interface BArchOnlineTimerReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BArchOnlineTimer copy();

    public String getAccount();
    public String getClientId();
    public Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly();

    public Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly();
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly();
    public long getLoginVersion();
}
