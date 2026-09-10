// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BTaskConfigReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BTaskConfig copy();
    BTaskConfig.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getTaskId();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getPreposeTasksReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getFollowTasksReadOnly();
    int getAcceptNpc();
    int getFinishNpc();
    Zeze.Transaction.DynamicBeanReadOnly getExtendDataReadOnly();
    metagame.builtin.TaskModule.BTaskReadOnly getTaskConditionsReadOnly();
    int getPreposeRequired();
    boolean isRepeatable();
}
