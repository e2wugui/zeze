// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BGetRoleTasksResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetRoleTasksResult copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.TaskModule.BTaskDescription, Zeze.Builtin.Game.TaskModule.BTaskDescriptionReadOnly> getTasksReadOnly();
}
