// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BLoginReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLogin copy();

    String getSessionName();
}
