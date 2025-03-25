// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

public interface BRegisterReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRegister copy();

    String getLinkName();
}
