// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BGetDataWithVersionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetDataWithVersion copy();

    Zeze.Net.Binary getKey();
}
