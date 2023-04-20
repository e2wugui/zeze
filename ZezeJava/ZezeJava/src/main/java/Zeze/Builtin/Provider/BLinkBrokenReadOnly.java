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
    Zeze.Builtin.Provider.BUserStateReadOnly getUserStateReadOnly();
}
