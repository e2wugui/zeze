// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BTaskReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTask copy();

    int getTaskId();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BPhase, Zeze.Builtin.Game.TaskModule.BPhaseReadOnly> getPhasesReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BCondition, Zeze.Builtin.Game.TaskModule.BConditionReadOnly> getConditionsReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getIndexSetReadOnly();
    int getTaskState();
    boolean isAutoCompleted();
    int getRewardId();
}
