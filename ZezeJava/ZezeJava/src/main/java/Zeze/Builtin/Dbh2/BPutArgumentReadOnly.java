// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BPutArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPutArgument copy();

    long getTransactionId();
    Zeze.Net.Binary getKey();
    Zeze.Net.Binary getValue();
}
