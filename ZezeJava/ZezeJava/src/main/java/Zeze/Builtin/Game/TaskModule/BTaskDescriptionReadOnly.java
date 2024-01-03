// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BTaskDescriptionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskDescription copy();

    int getTaskId();
    int getTaskState();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BCondition, Zeze.Builtin.Game.TaskModule.BConditionReadOnly> getConditionsReadOnly();
    int getRewardId();
    int getRewardType();
    Zeze.Net.Binary getRewardParam();
}
