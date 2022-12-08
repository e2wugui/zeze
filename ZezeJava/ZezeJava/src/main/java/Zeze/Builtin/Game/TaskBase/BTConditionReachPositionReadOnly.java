// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionReachPositionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionReachPosition copy();

    public int getDimension();
    public double getX();
    public double getY();
    public double getZ();
    public double getRadius();
    public boolean isReached();
}
