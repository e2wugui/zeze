// auto-generated @formatter:off
package Zeze.Builtin.World;

// 命令 eAoiLeaves 的参数。
public interface BAoiLeavesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiLeaves copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BCubeIndex, Zeze.Builtin.World.BCubeIndexReadOnly> getCubeIndexsReadOnly();
}
