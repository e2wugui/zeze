// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BTimerReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTimer copy();

    public String getTimerName();
    public String getHandleName();
    public Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly();

    public Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly();
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();

    public long getConcurrentFireSerialNo();
}
