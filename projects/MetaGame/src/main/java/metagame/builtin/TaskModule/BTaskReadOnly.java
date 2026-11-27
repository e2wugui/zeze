// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BTaskReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BTask copy();
    BTask.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Transaction.VariableMeta> variables();

    int getTaskId();
    Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BPhase, metagame.builtin.TaskModule.BPhaseReadOnly> getPhasesReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BCondition, metagame.builtin.TaskModule.BConditionReadOnly> getConditionsReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getIndexSetReadOnly();
    int getTaskState();
    boolean isAutoFinish();
    int getRewardId();
}
