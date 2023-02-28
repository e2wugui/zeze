// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BLocalsReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLocals copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BLocal, Zeze.Builtin.Online.BLocalReadOnly> getLoginsReadOnly();
}
