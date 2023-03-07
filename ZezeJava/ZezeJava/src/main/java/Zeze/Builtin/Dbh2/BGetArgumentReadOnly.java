// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BGetArgumentReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetArgument copy();

    String getDatabase();
    String getTable();
    Zeze.Net.Binary getKey();
}
