// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BMoveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMove copy();

    String getBagName();
    int getPositionFrom();
    int getPositionTo();
    int getNumber();
}
