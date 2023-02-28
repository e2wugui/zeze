// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 记录每个角色的任务数据
public interface BRoleTasksReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRoleTasks copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> getProcessingTasksReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Long> getFinishedTaskIdsReadOnly();
}
