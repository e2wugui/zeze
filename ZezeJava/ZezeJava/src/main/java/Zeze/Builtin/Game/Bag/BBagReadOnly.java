// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BBagReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BBag copy();

    public int getCapacity();
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.Bag.BItem, Zeze.Builtin.Game.Bag.BItemReadOnly> getItemsReadOnly();
}
