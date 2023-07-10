// auto-generated @formatter:off
package Zeze.Builtin.World;

// 命令 eAoiLeave 的参数。
public interface BAoiLeaveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiLeave copy();

    Zeze.Builtin.World.BCubeIndexReadOnly getCubeIndexReadOnly();
    Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Builtin.World.BObjectId> getKeysReadOnly();
}
