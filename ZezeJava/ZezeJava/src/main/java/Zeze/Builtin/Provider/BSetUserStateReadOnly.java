// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSetUserStateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSetUserState copy();

    long getLinkSid();
    Zeze.Builtin.Provider.BUserStateReadOnly getUserStateReadOnly();
}
