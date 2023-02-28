// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BBagReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBag copy();

    int getCapacity();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.Bag.BItem, Zeze.Builtin.Game.Bag.BItemReadOnly> getItemsReadOnly();
}
