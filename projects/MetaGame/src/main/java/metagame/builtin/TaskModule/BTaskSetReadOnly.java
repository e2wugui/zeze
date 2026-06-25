// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BTaskSetReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskSet copy();

    Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getTaskIdsReadOnly();
}
