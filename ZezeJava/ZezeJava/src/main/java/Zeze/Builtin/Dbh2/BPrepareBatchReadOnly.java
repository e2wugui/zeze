// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BPrepareBatchReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPrepareBatch copy();

    String getMaster();
    String getDatabase();
    String getTable();
    Zeze.Builtin.Dbh2.BBatchReadOnly getBatchReadOnly();
}
