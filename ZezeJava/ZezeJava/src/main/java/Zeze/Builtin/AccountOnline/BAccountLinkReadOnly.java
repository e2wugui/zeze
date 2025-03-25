// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

public interface BAccountLinkReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAccountLink copy();

    String getAccount();
    String getLinkName();
    long getLinkSid();
}
