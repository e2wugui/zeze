// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public interface BPrepareBatchesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPrepareBatches copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Dbh2.BPrepareBatch, Zeze.Builtin.Dbh2.BPrepareBatchReadOnly> getDatasReadOnly();
    String getQueryIp();
    int getQueryPort();
}
