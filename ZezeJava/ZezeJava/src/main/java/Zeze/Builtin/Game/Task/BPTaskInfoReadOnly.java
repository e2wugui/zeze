// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

// 记录每个角色的任务数据
public interface BPTaskInfoReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BPTaskInfo copy();

    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Long> getProcessingTaskReadOnly();
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getFinishedTaskReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getRoleTaskCustomDataReadOnly();

}
