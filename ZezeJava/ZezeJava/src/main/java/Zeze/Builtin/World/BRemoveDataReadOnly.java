// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BRemoveDataReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRemoveData copy();

    Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Builtin.World.ObjectId> getKeysReadOnly();
}
