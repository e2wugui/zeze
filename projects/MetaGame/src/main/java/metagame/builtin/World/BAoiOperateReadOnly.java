// auto-generated @formatter:off
package metagame.builtin.World;

// 一个具体的操作。
public interface BAoiOperateReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BAoiOperate copy();
    BAoiOperate.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getOperateId();
    Zeze.Net.Binary getParam();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BAoiOperate, metagame.builtin.World.BAoiOperateReadOnly> getChildrenReadOnly();
}
