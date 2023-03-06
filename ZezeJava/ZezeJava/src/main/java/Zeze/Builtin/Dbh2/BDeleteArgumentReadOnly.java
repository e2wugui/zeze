// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BDeleteArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDeleteArgument copy();

    long getTransactionId();
    String getDatabase();
    String getTable();
    Zeze.Net.Binary getKey();
}
