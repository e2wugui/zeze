// auto-generated @formatter:off
package metagame.builtin.World;

// 命令 eAoiLeave 的参数。
public interface BAoiLeavesReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BAoiLeaves copy();
    BAoiLeaves.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PList1ReadOnly<Long> getKeysReadOnly();
}
