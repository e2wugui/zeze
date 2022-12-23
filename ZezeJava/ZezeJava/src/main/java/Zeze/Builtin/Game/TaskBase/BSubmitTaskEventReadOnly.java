// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BSubmitTaskEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSubmitTaskEvent copy();

    public long getTaskId();
}
