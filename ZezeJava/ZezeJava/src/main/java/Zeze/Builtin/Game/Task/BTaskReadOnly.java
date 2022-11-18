// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTask copy();

    public String getTaskName();
    public int getState();
    public String getCurrentPhaseId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> getTaskPhasesReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskCustomDataReadOnly();

}
