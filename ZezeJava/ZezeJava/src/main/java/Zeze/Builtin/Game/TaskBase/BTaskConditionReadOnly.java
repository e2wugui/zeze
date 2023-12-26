// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 允许广播
public interface BTaskConditionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskCondition copy();

    String getConditionType();
    Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();
}
