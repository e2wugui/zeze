// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// TODO: 允许广播
public interface BTaskConditionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskCondition copy();

    public String getConditionType();
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();

}
