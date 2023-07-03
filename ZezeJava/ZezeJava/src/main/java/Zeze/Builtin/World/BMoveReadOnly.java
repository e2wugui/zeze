// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BMoveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMove copy();

    Zeze.Serialize.Vector3 getCurrent();
    Zeze.Serialize.Vector3 getDirect();
    int getTurnDirect();
    long getTimestamp();
}
