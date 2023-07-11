// auto-generated @formatter:off
package Zeze.Builtin.World;

// 一个具体的操作。
public interface BAoiOperateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiOperate copy();

    int getOperateId();
    Zeze.Net.Binary getParam();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.World.BAoiOperate, Zeze.Builtin.World.BAoiOperateReadOnly> getChildrenReadOnly();
}
