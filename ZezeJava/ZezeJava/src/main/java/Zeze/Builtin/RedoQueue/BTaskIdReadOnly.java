// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

public interface BTaskIdReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskId copy();

    long getTaskId();
}
