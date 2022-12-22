// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTask copy();

    public long getRoleId();
    public long getTaskId();
    public String getTaskType();
    public int getTaskState();
    public String getTaskName();
    public String getTaskDescription();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPreTaskIdsReadOnly();
    public long getCurrentPhaseId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTaskPhase, Zeze.Builtin.Game.TaskBase.BTaskPhaseReadOnly> getTaskPhasesReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();

}
