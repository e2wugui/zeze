// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTask copy();

    public long getTaskId();
    public int getState();
    public long getCurrentPhaseId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> getTaskPhasesReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskCustomDataReadOnly();

}
