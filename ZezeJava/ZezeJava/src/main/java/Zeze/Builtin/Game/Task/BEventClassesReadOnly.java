// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public interface BEventClassesReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BEventClasses copy();

    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getEventClassesReadOnly();
}
