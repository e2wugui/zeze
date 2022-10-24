// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BItemClassesReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BItemClasses copy();

    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getItemClassesReadOnly();
}
