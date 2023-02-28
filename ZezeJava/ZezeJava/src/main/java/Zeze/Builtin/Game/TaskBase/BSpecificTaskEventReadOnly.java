// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BSpecificTaskEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSpecificTaskEvent copy();

    long getTaskId();
}
