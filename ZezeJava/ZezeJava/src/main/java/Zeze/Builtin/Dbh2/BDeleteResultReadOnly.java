// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BDeleteResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDeleteResult copy();

    String getRaftConfig();
}
