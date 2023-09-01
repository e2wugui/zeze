// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BTransmitSimpleTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransmitSimpleTimer copy();

    String getTimerId();
    Zeze.Builtin.Timer.BSimpleTimerReadOnly getSimpleTimerReadOnly();
    String getHandleClass();
    String getCustomClass();
    Zeze.Net.Binary getCustomBean();
    long getLoginVersion();
    boolean isHot();
}
