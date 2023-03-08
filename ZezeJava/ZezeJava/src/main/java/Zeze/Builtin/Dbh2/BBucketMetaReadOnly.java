// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BBucketMetaReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBucketMeta copy();

    String getDatabaseName();
    String getTableName();
    Zeze.Net.Binary getKeyFirst();
    Zeze.Net.Binary getKeyLast();
    String getRaftConfig();
    boolean isMoving();
    Zeze.Net.Binary getKeyMoving();
}
