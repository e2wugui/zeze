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
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BCubePutData, Zeze.Builtin.World.BCubePutDataReadOnly> getPriorityDataReadOnly();
}
