// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BNodeRootReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BNodeRoot copy();

    public long getHeadNodeId();
    public long getTailNodeId();
    public long getLoadSerialNo();
}
