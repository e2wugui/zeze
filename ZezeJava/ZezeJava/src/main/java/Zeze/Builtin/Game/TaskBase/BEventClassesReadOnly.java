// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BEventClassesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BEventClasses copy();

    Zeze.Transaction.Collections.PSet1ReadOnly<String> getEventClassesReadOnly();
}
