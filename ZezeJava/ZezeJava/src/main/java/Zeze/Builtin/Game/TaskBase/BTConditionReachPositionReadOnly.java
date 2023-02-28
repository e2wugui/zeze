// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：到达位置
public interface BTConditionReachPositionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionReachPosition copy();

    int getDimension();
    double getX();
    double getY();
    double getZ();
    double getRadius();
    boolean isReached();
}
