// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BCreateTableReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCreateTable copy();

    String getDatabase();
    String getTable();
}
