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
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPrePhaseIdsReadOnly();
    public long getNextPhaseId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BSubPhase, Zeze.Builtin.Game.TaskBase.BSubPhaseReadOnly> getSubPhasesReadOnly();
}
