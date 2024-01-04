// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BTaskDescriptionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskDescription copy();

    int getTaskId();
    int getTaskState();
    String getPhaseDescription();
    Zeze.Transaction.Collections.PList1ReadOnly<String> getPhaseConditionsReadOnly();
    Zeze.Transaction.Collections.PList1ReadOnly<String> getConditionsReadOnly();
    int getRewardId();
    int getRewardType();
    Zeze.Net.Binary getRewardParam();
}
