// auto-generated @formatter:off
package metagame.builtin.World;

// 一个具体的操作。
public interface BAoiOperateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiOperate copy();

    int getOperateId();
    Zeze.Net.Binary getParam();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BAoiOperate, metagame.builtin.World.BAoiOperateReadOnly> getChildrenReadOnly();
}
