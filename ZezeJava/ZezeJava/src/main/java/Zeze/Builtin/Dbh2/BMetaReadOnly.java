// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BMetaReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMeta copy();

    String getDatabaseName();
    String getTableName();
    Zeze.Net.Binary getKeyFirst();
    Zeze.Net.Binary getKeyLast();
}
