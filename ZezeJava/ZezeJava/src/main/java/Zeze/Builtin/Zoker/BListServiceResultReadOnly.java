// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public interface BListServiceResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BListServiceResult copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Zoker.BService, Zeze.Builtin.Zoker.BServiceReadOnly> getServicesReadOnly();
}
