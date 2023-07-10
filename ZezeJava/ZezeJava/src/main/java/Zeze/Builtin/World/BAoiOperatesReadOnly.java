// auto-generated @formatter:off
package Zeze.Builtin.World;

// 命令 eAoiEnter,eAoiOperate的参数。
public interface BAoiOperatesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiOperates copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Zeze.Builtin.World.BObjectId, Zeze.Builtin.World.BAoiOperate, Zeze.Builtin.World.BAoiOperateReadOnly> getOperatesReadOnly();
}
