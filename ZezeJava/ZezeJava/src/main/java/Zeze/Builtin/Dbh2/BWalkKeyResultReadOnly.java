// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BWalkKeyResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BWalkKeyResult copy();

    Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getKeysReadOnly();
    boolean isBucketEnd();
    boolean isBucketRefuse();
}
