// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

// TaskCondition的Bean数据，只存在在BTaskPhase之内
public interface BTaskConditionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskCondition copy();

    public long getTaskConditionId();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskConditionCustomDataReadOnly();

}
