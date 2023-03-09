// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BPutResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPutResult copy();

    String getRaftConfig();
}
