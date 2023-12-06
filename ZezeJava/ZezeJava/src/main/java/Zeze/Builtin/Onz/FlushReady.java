// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class FlushReady extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BFlushReady.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -2143186614; // 2151780682
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47410000793930
    static { register(TypeId_, FlushReady.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public FlushReady() {
        Argument = new Zeze.Builtin.Onz.BFlushReady.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public FlushReady(Zeze.Builtin.Onz.BFlushReady.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
