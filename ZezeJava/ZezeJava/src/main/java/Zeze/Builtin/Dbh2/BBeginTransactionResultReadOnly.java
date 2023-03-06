// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BBeginTransactionResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBeginTransactionResult copy();

    long getTransactionId();
}
