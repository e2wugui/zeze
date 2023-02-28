// auto-generated @formatter:off
package Zeze.Builtin.AutoKey;

public interface BAutoKeyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAutoKey copy();

    long getNextId();
}
