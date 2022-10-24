// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

public interface BTaskIdReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskId copy();

    public long getTaskId();
}
