// auto-generated @formatter:off
package metagame.builtin.World;

// 命令 eAoiEnter,eAoiOperate的参数。
public interface BAoiOperatesReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BAoiOperates copy();
    BAoiOperates.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BAoiOperate, metagame.builtin.World.BAoiOperateReadOnly> getOperatesReadOnly();
}
