// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public interface BQueryReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQuery copy();

    Zeze.Net.Binary getTransactionKey();
}
