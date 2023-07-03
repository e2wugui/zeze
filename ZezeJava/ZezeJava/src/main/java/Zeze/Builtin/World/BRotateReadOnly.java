// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BRotateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRotate copy();

    Zeze.Serialize.Vector3 getCurrent();
    Zeze.Serialize.Vector3 getDirect();
    boolean isTurnDirect();
    long getTimestamp();
}
