// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public interface BTaskSetReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskSet copy();

    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getTaskIdsReadOnly();
}
