// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// protocols
public interface BLoginReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLogin copy();

    long getRoleId();
}
