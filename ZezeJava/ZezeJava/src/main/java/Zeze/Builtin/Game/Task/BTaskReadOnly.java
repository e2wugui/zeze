// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTask copy();

    public String getTaskId();
    public String getTaskName();
    public Zeze.Builtin.Game.Task.BTaskPhaseReadOnly getCurrentPhaseReadOnly();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> getTaskPhasesReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskCustomDataReadOnly();

}
