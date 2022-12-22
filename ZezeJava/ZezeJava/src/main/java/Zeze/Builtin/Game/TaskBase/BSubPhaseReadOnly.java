// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BSubPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSubPhase copy();

    public long getSubPhaseId();
    public int getCompleteType();
    public int getNextSubPhaseId();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly();
}
