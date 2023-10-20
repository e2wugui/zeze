// auto-generated @formatter:off
package Zeze.Builtin.Auth;

public interface BRoleAuthReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRoleAuth copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Long, String> getAuthsReadOnly();
}
