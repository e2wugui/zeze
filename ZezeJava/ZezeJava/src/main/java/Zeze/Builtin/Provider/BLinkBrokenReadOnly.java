// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BLinkBrokenReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLinkBroken copy();

    public String getAccount();
    public long getLinkSid();
    public int getReason();
    public String getContext();
    public Zeze.Net.Binary getContextx();
}
