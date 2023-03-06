// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BGetResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetResult copy();

    boolean isNull();
    Zeze.Net.Binary getValue();
}
