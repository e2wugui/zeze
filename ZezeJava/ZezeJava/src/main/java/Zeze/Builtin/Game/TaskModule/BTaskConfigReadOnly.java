// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BTaskConfigReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskConfig copy();

    int getTaskId();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getPreposeTasksReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getFollowTasksReadOnly();
    int getAcceptNpc();
    int getFinishNpc();
    Zeze.Transaction.DynamicBeanReadOnly getExtendDataReadOnly();
    Zeze.Builtin.Game.TaskModule.BTaskReadOnly getTaskConditionsReadOnly();
    int getPreposeRequired();
    boolean isRepeatable();
}
