// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSubscribeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSubscribe copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Provider.BModule, Zeze.Builtin.Provider.BModuleReadOnly> getModulesReadOnly();
}
