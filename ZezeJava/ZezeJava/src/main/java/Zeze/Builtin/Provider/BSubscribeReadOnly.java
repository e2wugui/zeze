// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSubscribeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSubscribe copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Provider.BModule, Zeze.Builtin.Provider.BModuleReadOnly> getModulesReadOnly();
}
