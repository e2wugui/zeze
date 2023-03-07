// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BBeginTransactionArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBeginTransactionArgument copy();

    String getDatabase();
    String getTable();
}
