// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BPutDataReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPutData copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BEditData, Zeze.Builtin.World.BEditDataReadOnly> getDatasReadOnly();
}
