// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTaskReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTask copy();

    long getRoleId();
    long getTaskId();
    String getTaskType();
    int getTaskState();
    String getTaskName();
    String getTaskDescription();
    Zeze.Transaction.Collections.PList1ReadOnly<Long> getPreTaskIdsReadOnly();
    long getCurrentPhaseId();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTaskPhase, Zeze.Builtin.Game.TaskBase.BTaskPhaseReadOnly> getTaskPhasesReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();
}
