// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// ======================================== TaskPhase的Bean数据 ========================================
public interface BTaskPhaseReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskPhase copy();

    long getPhaseId();
    String getPhaseName();
    String getPhaseDescription();
    Zeze.Transaction.Collections.PList1ReadOnly<Long> getPrePhaseIdsReadOnly();
    long getNextPhaseId();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BSubPhase, Zeze.Builtin.Game.TaskBase.BSubPhaseReadOnly> getSubPhasesReadOnly();
    long getCurrentSubPhaseId();
}
