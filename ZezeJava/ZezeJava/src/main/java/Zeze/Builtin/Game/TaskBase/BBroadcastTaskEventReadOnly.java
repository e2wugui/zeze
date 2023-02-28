// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BBroadcastTaskEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBroadcastTaskEvent copy();

    boolean isIsBreakIfAccepted();
}
