// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// link to gs
public interface BDispatchReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDispatch copy();

    long getLinkSid();
    String getAccount();
    long getProtocolType();
    Zeze.Net.Binary getProtocolData();
    String getContext();
    Zeze.Net.Binary getContextx();
    String getOnlineSetName();
}
