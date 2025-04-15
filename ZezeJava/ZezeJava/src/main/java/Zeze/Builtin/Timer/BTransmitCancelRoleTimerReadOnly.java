// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BTransmitCancelRoleTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransmitCancelRoleTimer copy();

    String getTimerId();
    long getRoleId();
    long getLoginVersion();
}
