// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskPhase copy();

    public long getTaskPhaseId();
    public long getCurrentConditionId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.Task.BTaskCondition, Zeze.Builtin.Game.Task.BTaskConditionReadOnly> getTaskConditionsReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskPhaseCustomDataReadOnly();

}
