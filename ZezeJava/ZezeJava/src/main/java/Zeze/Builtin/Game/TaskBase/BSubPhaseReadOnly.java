// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BSubPhaseReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSubPhase copy();

    long getSubPhaseId();
    String getCompleteType();
    long getNextSubPhaseId();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly();
}
