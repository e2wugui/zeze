// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BDelayLogoutCustomReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDelayLogoutCustom copy();

    String getAccount();
    String getClientId();
    long getLoginVersion();
}
