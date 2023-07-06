// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BEnterWorldReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BEnterWorld copy();

    int getMapId();
    long getMapInstanceId();
    Zeze.Serialize.Vector3 getPosition();
    Zeze.Builtin.World.BPutDataReadOnly getPriorityDataReadOnly();
}
