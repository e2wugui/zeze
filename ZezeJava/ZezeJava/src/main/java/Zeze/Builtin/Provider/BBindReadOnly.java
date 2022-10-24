// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BBindReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BBind copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Provider.BModule, Zeze.Builtin.Provider.BModuleReadOnly> getModulesReadOnly();
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getLinkSidsReadOnly();
}
