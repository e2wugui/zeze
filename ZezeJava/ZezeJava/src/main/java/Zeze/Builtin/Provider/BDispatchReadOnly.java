// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// link to gs
public interface BDispatchReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDispatch copy();

    public long getLinkSid();
    public String getAccount();
    public long getProtocolType();
    public Zeze.Net.Binary getProtocolData();
    public String getContext();
    public Zeze.Net.Binary getContextx();
}
