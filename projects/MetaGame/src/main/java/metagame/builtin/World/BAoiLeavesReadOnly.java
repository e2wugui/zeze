// auto-generated @formatter:off
package metagame.builtin.World;

// 命令 eAoiLeave 的参数。
public interface BAoiLeavesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiLeaves copy();

    Zeze.Transaction.Collections.PList1ReadOnly<Long> getKeysReadOnly();
}
