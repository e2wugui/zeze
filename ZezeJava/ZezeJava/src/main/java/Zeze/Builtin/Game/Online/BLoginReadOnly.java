// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// protocols
public interface BLoginReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLogin copy();

    public long getRoleId();
}
