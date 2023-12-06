// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class Ready extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BReady.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -1374796664; // 2920170632
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47410769183880
    static { register(TypeId_, Ready.class); }

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

    public Ready() {
        Argument = new Zeze.Builtin.Onz.BReady.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Ready(Zeze.Builtin.Onz.BReady.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
