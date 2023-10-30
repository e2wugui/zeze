// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BBatchTidReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBatchTid copy();

    long getTid();
}
