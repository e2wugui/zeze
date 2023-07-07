// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BCubeRemoveDataReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCubeRemoveData copy();

    Zeze.Builtin.World.BCubeIndexReadOnly getCubeIndexReadOnly();
    Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Builtin.World.BObjectId> getKeysReadOnly();
}
