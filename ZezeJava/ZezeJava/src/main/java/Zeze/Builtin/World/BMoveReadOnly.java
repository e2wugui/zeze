// auto-generated @formatter:off
package Zeze.Builtin.World;

// MoveMmo
public interface BMoveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMove copy();

    Zeze.Serialize.Vector3 getPosition();
    Zeze.Serialize.Vector3 getDirect();
    int getCommand();
    long getTimestamp();
}
