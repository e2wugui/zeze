// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BEditDataReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BEditData copy();

    Zeze.Builtin.World.BObjectId getObjectId();
    int getEditId();
    Zeze.Net.Binary getData();
}
