// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTask copy();

    public String getTaskName();
    public int getTaskType();
    public int getState();
    public String getCurrentPhaseId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.TaskBase.BTaskPhase, Zeze.Builtin.Game.TaskBase.BTaskPhaseReadOnly> getTaskPhasesReadOnly();
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getPreTasksReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskCustomDataReadOnly();

}
