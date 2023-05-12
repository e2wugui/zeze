// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BBucketCountReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBucketCount copy();

    int getCount();
}
