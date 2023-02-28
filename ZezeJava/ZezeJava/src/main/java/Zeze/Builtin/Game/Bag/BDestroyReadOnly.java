// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BDestroyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDestroy copy();

    String getBagName();
    int getPosition();
}
