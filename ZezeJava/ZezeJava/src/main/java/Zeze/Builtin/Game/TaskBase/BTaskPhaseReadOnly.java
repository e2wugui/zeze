// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// ======================================== TaskPhase的Bean数据 ========================================
public interface BTaskPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskPhase copy();

    public long getPhaseId();
    public String getPhaseName();
    public String getPhaseDescription();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getAfterPhaseIdsReadOnly();
    public long getNextPhaseId();
    public int getCommitType();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly();
    public int getConditionsCompleteType();
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();

    public Zeze.Builtin.Game.TaskBase.BTPhaseCommitNPCTalkReadOnly getExtendedData_Zeze_Builtin_Game_TaskBase_BTPhaseCommitNPCTalkReadOnly();
}
