// auto-generated @formatter:off
package metagame.builtin.World;

// 命令 eAoiEnter,eAoiOperate的参数。
public interface BAoiOperatesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiOperates copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BAoiOperate, metagame.builtin.World.BAoiOperateReadOnly> getOperatesReadOnly();
}
