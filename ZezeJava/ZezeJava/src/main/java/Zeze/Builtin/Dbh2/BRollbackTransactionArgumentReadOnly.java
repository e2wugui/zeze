// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BRollbackTransactionArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRollbackTransactionArgument copy();

    long getTransactionId();
}
