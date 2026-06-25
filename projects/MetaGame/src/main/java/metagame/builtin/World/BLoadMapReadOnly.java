// auto-generated @formatter:off
package metagame.builtin.World;

public interface BLoadMapReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoadMap copy();

    int getMapId();
    metagame.builtin.World.BLoadReadOnly getLoadSumReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BLoad, metagame.builtin.World.BLoadReadOnly> getInstancesReadOnly();
}
