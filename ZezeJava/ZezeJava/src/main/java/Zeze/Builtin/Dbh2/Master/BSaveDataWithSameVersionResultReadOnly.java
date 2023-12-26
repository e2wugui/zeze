// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BSaveDataWithSameVersionResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSaveDataWithSameVersionResult copy();

    long getVersion();
}
