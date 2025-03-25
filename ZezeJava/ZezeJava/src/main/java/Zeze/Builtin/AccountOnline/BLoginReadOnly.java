// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

public interface BLoginReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLogin copy();

    Zeze.Builtin.AccountOnline.BAccountLinkReadOnly getAccountLinkReadOnly();
    boolean isKickOld();
}
