// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionSubmitItemEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionSubmitItemEvent copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsReadOnly();
}
