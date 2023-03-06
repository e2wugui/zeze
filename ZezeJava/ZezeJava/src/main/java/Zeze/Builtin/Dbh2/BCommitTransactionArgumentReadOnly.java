// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BCommitTransactionArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCommitTransactionArgument copy();

    long getTransactionId();
}
