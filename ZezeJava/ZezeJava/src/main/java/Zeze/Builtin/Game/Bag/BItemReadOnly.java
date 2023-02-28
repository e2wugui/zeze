// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BItemReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BItem copy();

    int getId();
    int getNumber();
    Zeze.Transaction.DynamicBeanReadOnly getItemReadOnly();
}
