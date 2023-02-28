// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BIndexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BIndex copy();

    int getServerId();
    long getNodeId();
}
