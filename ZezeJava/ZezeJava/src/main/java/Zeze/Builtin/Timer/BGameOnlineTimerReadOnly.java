// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 保存在内存Map中
public interface BGameOnlineTimerReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BGameOnlineTimer copy();

    public long getRoleId();
    public Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly();

    public Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly();
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly();
    public long getLoginVersion();
}
