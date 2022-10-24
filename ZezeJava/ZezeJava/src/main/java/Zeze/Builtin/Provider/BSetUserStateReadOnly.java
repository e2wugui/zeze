// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSetUserStateReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSetUserState copy();

    public long getLinkSid();
    public String getContext();
    public Zeze.Net.Binary getContextx();
}
