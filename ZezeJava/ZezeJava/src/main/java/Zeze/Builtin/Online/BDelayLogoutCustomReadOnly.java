// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BDelayLogoutCustomReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDelayLogoutCustom copy();

    public String getAccount();
    public String getClientId();
    public long getLoginVersion();
}
