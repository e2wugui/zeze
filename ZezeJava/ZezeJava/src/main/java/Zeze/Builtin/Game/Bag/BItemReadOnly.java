// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BItemReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BItem copy();

    public int getId();
    public int getNumber();
    public Zeze.Transaction.DynamicBeanReadOnly getItemReadOnly();

}
