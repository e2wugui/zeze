// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BWalkKeyValueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BWalkKeyValue copy();

    Zeze.Net.Binary getKey();
    Zeze.Net.Binary getValue();
}
