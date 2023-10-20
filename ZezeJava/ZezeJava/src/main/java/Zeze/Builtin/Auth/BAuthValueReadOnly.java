// auto-generated @formatter:off
package Zeze.Builtin.Auth;

public interface BAuthValueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAuthValue copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Builtin.Auth.BAuthKey, String> getAuthsReadOnly();
}
