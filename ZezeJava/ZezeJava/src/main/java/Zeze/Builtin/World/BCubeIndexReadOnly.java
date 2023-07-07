// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BCubeIndexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCubeIndex copy();

    long getX();
    long getY();
    long getZ();
}
