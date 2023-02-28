// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BBindReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBind copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Provider.BModule, Zeze.Builtin.Provider.BModuleReadOnly> getModulesReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Long> getLinkSidsReadOnly();
}
