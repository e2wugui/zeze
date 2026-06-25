// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BRoleTasksReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRoleTasks copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, metagame.builtin.TaskModule.BTask, metagame.builtin.TaskModule.BTaskReadOnly> getTasksReadOnly();
}
