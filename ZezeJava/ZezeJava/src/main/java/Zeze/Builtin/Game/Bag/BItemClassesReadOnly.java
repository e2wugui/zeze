// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BItemClassesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BItemClasses copy();

    Zeze.Transaction.Collections.PSet1ReadOnly<String> getItemClassesReadOnly();
}
