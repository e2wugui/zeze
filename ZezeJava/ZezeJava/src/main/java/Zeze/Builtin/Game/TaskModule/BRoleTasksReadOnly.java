// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BRoleTasksReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRoleTasks copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.TaskModule.BTask, Zeze.Builtin.Game.TaskModule.BTaskReadOnly> getTasksReadOnly();
}
