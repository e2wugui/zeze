// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// <enum name="ConditionCompleteBranch" value="24"/> 通过不同的完成条件实现进入不同的分支Phase
public interface BTaskPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskPhase copy();

    public long getPhaseId();
    public int getPhaseType();
    public String getPhaseName();
    public String getPhaseDescription();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getAfterPhaseIdsReadOnly();
    public long getNextPhaseId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly();
    public int getConditionsCompleteType();
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();

    public Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalkReadOnly getExtendedData_Zeze_Builtin_Game_TaskBase_BTaskPhaseCommitNPCTalkReadOnly();
}
