// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BEndSplitReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BEndSplit copy();

    Zeze.Builtin.Dbh2.BBucketMetaReadOnly getFromReadOnly();
    Zeze.Builtin.Dbh2.BBucketMetaReadOnly getToReadOnly();
}
