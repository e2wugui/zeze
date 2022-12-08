// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionReachPositionEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionReachPositionEvent copy();

    public double getX();
    public double getY();
    public double getZ();
}
