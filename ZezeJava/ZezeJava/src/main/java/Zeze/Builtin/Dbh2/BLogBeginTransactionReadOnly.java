// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BLogBeginTransactionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLogBeginTransaction copy();

    long getTransactionId();
}
