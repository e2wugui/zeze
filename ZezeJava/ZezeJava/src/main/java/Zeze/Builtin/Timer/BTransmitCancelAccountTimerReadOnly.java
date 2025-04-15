// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BTransmitCancelAccountTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransmitCancelAccountTimer copy();

    String getTimerId();
    String getAccount();
    String getClientId();
    long getLoginVersion();
}
