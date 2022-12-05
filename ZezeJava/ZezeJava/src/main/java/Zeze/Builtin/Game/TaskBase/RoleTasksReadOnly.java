// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 记录每个角色的任务数据
public interface RoleTasksReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public RoleTasks copy();

    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getAvailableTasksIdReadOnly();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getProcessingTasksIdReadOnly();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getFinishedTaskIdReadOnly();
}
