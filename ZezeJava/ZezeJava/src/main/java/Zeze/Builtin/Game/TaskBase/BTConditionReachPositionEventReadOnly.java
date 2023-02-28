// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionReachPositionEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionReachPositionEvent copy();

    double getX();
    double getY();
    double getZ();
}
