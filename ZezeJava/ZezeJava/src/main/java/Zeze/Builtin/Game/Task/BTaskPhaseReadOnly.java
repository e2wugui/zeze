// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskPhase copy();

    public String getTaskPhaseId();
    public String getTaskPhaseName();
    public Zeze.Builtin.Game.Task.BTaskConditionReadOnly getCurrentConditionReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskPhaseCustomDataReadOnly();

}
