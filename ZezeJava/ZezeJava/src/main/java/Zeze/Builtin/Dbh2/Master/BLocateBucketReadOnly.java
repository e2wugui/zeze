// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BLocateBucketReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLocateBucket copy();

    String getDatabase();
    String getTable();
    Zeze.Net.Binary getKey();
}
