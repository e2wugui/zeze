// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BAoiOperatesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiOperates copy();

    Zeze.Builtin.World.BCubeIndexReadOnly getCubeIndexReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BAoiOperate, Zeze.Builtin.World.BAoiOperateReadOnly> getOperatesReadOnly();
}
