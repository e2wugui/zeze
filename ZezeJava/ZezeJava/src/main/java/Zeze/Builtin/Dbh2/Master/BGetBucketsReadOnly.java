// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BGetBucketsReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetBuckets copy();

    String getDatabase();
    String getTable();
}
