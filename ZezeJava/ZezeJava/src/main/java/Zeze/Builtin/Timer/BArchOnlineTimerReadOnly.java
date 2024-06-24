// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 用于Zeze.Timer.tAccountTimers内存表的value, 只处理账号在线时的定时器
public interface BArchOnlineTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BArchOnlineTimer copy();

    String getAccount();
    String getClientId();
    Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly();
    Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly();
    Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly();
    long getLoginVersion();
    long getSerialId();
}
