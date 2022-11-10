// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

// 内置条件类型
public interface BNamedCountConditionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BNamedCountCondition copy();

    public long getCurrentCount();
    public long getTargetCount();
}
