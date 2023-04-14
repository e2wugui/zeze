// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public interface BCommitReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCommit copy();

    Zeze.Net.Binary getTransactionData();
}
