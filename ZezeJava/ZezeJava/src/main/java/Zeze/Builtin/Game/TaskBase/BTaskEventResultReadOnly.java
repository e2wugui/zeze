// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTaskEventResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskEventResult copy();

    long getResultCode();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> getChangedTasksReadOnly();
}
