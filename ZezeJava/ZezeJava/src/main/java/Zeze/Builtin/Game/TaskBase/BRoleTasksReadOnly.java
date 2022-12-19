// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 记录每个角色的任务数据
public interface BRoleTasksReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BRoleTasks copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> getProcessingTasksReadOnly();
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getFinishedTaskIdsReadOnly();
}
