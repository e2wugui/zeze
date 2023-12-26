// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BGetDataWithVersionResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetDataWithVersionResult copy();

    Zeze.Net.Binary getData();
    long getVersion();
}
