// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BLinkBrokenReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLinkBroken copy();

    String getAccount();
    long getLinkSid();
    int getReason();
    String getContext();
    Zeze.Net.Binary getContextx();
}
