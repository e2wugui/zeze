// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// TaskCondition的Bean数据，只存在在BTaskPhase之内
public interface BTaskConditionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskCondition copy();

    public String getTaskConditionName();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskConditionCustomDataReadOnly();

}
