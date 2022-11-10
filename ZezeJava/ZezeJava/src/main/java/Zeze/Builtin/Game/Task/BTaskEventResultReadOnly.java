// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BTaskEventResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskEventResult copy();

    public boolean isSuccess();
}
