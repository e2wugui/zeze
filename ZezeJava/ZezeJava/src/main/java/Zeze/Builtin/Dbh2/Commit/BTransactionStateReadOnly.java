// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public interface BTransactionStateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransactionState copy();

    int getState();
    Zeze.Transaction.Collections.PList1ReadOnly<String> getBucketsReadOnly();
}
