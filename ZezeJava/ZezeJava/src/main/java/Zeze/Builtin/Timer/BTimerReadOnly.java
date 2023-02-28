// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTimer copy();

    String getTimerName();
    String getHandleName();
    Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly();
    Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly();
    Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
    long getConcurrentFireSerialNo();
}
