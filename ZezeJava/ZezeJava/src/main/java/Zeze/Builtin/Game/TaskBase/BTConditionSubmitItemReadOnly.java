// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionSubmitItemReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionSubmitItem copy();

    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsReadOnly();
    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getItemsSubmittedReadOnly();
}
