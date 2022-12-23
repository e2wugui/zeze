// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTaskEventResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskEventResult copy();

    public long getResultCode();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> getChangedTasksReadOnly();
}
