// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 保存在内存Map中
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
}
