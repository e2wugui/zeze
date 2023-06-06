// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BWalkResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BWalkResult copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Dbh2.BWalkKeyValue, Zeze.Builtin.Dbh2.BWalkKeyValueReadOnly> getKeyValuesReadOnly();
    boolean isBucketEnd();
    boolean isBucketRefuse();
}
