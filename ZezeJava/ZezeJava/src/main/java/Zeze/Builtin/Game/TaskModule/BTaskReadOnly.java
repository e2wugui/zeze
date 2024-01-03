// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BTaskReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTask copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BPhase, Zeze.Builtin.Game.TaskModule.BPhaseReadOnly> getPhasesReadOnly();
}
