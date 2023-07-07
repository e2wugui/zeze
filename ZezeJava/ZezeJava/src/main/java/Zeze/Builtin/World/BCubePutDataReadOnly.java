// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BCubePutDataReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCubePutData copy();

    Zeze.Builtin.World.BCubeIndexReadOnly getCubeIndexReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BEditData, Zeze.Builtin.World.BEditDataReadOnly> getDatasReadOnly();
}
