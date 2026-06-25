// auto-generated @formatter:off
package metagame.builtin.World.Static;

public interface BSwitchWorldReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSwitchWorld copy();

    int getMapId();
    int getFromMapId();
    int getFromGateId();
}
