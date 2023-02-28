// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：提交物品
public interface BTConditionSubmitItemReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionSubmitItem copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsReadOnly();
    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsSubmittedReadOnly();
}
