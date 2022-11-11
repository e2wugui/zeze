// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BPTaskInfoReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BPTaskInfo copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.Task.BTask, Zeze.Builtin.Game.Task.BTaskReadOnly> getProcessingTaskReadOnly();
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getFinishedTaskReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getRoleTaskCustomDataReadOnly();

}
