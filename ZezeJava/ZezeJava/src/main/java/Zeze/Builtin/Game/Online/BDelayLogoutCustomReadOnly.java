// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BDelayLogoutCustomReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDelayLogoutCustom copy();

    long getRoleId();
    long getLoginVersion();
}
