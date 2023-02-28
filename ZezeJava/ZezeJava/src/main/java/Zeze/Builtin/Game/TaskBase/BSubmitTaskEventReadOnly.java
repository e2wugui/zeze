// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BSubmitTaskEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSubmitTaskEvent copy();

    long getTaskId();
}
