// auto-generated @formatter:off
package Zeze.Builtin.World.Static;

public interface BSwitchWorldReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSwitchWorld copy();

    int getMapId();
    Zeze.Serialize.Vector3 getPosition();
    Zeze.Serialize.Vector3 getDirect();
}
