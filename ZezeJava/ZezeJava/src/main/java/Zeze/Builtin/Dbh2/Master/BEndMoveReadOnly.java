// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BEndMoveReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BEndMove copy();

    Zeze.Builtin.Dbh2.BBucketMetaReadOnly getToReadOnly();
}
