// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BLoadMapReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoadMap copy();

    int getMapId();
    Zeze.Builtin.World.BLoadReadOnly getLoadSumReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.World.BLoad, Zeze.Builtin.World.BLoadReadOnly> getInstancesReadOnly();
}
