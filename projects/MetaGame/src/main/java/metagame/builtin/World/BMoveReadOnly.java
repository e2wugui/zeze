// auto-generated @formatter:off
package metagame.builtin.World;

// MoveMmo
public interface BMoveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMove copy();

    Zeze.Serialize.Vector3 getPosition();
    Zeze.Serialize.Vector3 getDirect();
    int getState();
    int getControl();
    long getTimestamp();
}
