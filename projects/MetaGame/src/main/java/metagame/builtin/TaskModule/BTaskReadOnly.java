// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BTaskReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTask copy();

    int getTaskId();
    Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BPhase, metagame.builtin.TaskModule.BPhaseReadOnly> getPhasesReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BCondition, metagame.builtin.TaskModule.BConditionReadOnly> getConditionsReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getIndexSetReadOnly();
    int getTaskState();
    boolean isAutoFinish();
    int getRewardId();
}
