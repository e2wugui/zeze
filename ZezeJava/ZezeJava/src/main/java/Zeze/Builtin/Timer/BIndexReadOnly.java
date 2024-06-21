// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 一个timer的信息
public interface BIndexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BIndex copy();

    int getServerId();
    long getNodeId();
    long getSerialId();
}
