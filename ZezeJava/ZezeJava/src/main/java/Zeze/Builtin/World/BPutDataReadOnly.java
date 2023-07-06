// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BPutDataReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPutData copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Builtin.World.ObjectId, Zeze.Net.Binary> getDatasReadOnly();
}
