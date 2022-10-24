// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BMoveReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BMove copy();

    public String getBagName();
    public int getPositionFrom();
    public int getPositionTo();
    public int getNumber();
}
