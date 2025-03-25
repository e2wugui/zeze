// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

// 断线时自动登出。
public class Logout extends Zeze.Net.Rpc<Zeze.Builtin.AccountOnline.BAccountLink.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11041;
    public static final int ProtocolId_ = 1723066623;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47422456981759
    static { register(TypeId_, Logout.class); }

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

    public Logout() {
        Argument = new Zeze.Builtin.AccountOnline.BAccountLink.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Logout(Zeze.Builtin.AccountOnline.BAccountLink.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
