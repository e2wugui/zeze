// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public interface BTryDistributeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTryDistribute copy();

    long getDistributeId();
    boolean isAtomicAll();
}
