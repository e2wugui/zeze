// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BBroadcastTaskEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BBroadcastTaskEvent copy();

    public boolean isIsBreakIfAccepted();
}
