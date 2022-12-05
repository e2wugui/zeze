// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTaskPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskPhase copy();

    public long getPhaseId();
    public int getPhaseType();
    public String getPhaseName();
    public String getPhaseDescription();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPrePhasesIdReadOnly();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();

}
