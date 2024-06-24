// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 用于Zeze.Game.Online.tRoleTimers内存表的value, 只处理玩家在线时的定时器
public interface BGameOnlineTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGameOnlineTimer copy();

    long getRoleId();
    Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly();
    Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly();
    Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly();
    long getLoginVersion();
    long getSerialId();
}
