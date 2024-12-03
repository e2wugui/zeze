// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// Timer根节点
public interface BNodeRootReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BNodeRoot copy();

    long getHeadNodeId();
    long getTailNodeId();
    long getLoadSerialNo();
    long getVersion();
}
