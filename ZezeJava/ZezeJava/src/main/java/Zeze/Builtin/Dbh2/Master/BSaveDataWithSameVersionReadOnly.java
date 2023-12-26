// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BSaveDataWithSameVersionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSaveDataWithSameVersion copy();

    Zeze.Net.Binary getKey();
    Zeze.Net.Binary getData();
    long getVersion();
}
