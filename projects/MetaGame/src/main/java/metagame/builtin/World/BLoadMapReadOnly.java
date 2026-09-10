// auto-generated @formatter:off
package metagame.builtin.World;

public interface BLoadMapReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BLoadMap copy();
    BLoadMap.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getMapId();
    metagame.builtin.World.BLoadReadOnly getLoadSumReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BLoad, metagame.builtin.World.BLoadReadOnly> getInstancesReadOnly();
}
