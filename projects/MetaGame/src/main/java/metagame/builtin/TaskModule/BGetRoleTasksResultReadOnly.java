// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BGetRoleTasksResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetRoleTasksResult copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, metagame.builtin.TaskModule.BTaskDescription, metagame.builtin.TaskModule.BTaskDescriptionReadOnly> getTasksReadOnly();
}
