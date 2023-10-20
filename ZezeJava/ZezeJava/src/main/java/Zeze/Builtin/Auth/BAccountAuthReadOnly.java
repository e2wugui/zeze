// auto-generated @formatter:off
package Zeze.Builtin.Auth;

public interface BAccountAuthReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAccountAuth copy();

    Zeze.Transaction.Collections.PSet1ReadOnly<String> getRolesReadOnly();
}
