// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BAcceptTaskEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAcceptTaskEvent copy();

    long getTaskId();
}
