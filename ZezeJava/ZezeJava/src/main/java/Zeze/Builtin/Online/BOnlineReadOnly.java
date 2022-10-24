// auto-generated @formatter:off
package Zeze.Builtin.Online;

// tables
public interface BOnlineReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOnline copy();

    public String getLinkName();
    public long getLinkSid();
}
