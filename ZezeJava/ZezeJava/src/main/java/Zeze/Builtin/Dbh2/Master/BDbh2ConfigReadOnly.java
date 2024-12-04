// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BDbh2ConfigReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDbh2Config copy();

    String getDatabase();
    String getTable();
    String getRaftConfig();
}
