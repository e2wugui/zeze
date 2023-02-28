// auto-generated @formatter:off
package Zeze.Builtin.Online;

// protocols
public interface BLoginReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLogin copy();

    String getClientId();
}
