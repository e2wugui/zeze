// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BTransmitCronTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransmitCronTimer copy();

    String getTimerId();
    Zeze.Builtin.Timer.BCronTimerReadOnly getCronTimerReadOnly();
    String getHandleClass();
    String getCustomClass();
    Zeze.Net.Binary getCustomBean();
    long getLoginVersion();
    boolean isHot();
}
