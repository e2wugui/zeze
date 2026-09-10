// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BPhaseReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BPhase copy();
    BPhase.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BCondition, metagame.builtin.TaskModule.BConditionReadOnly> getConditionsReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getIndexSetReadOnly();
    String getDescription();
}
