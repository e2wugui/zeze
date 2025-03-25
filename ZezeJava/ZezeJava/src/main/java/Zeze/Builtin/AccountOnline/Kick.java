// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

// 重复登录并且需要踢掉旧的登录。
public class Kick extends Zeze.Net.Rpc<Zeze.Builtin.AccountOnline.BAccountLink.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11041;
    public static final int ProtocolId_ = -341241995; // 3953725301
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47424687640437
    static { register(TypeId_, Kick.class); }

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

    public Kick() {
        Argument = new Zeze.Builtin.AccountOnline.BAccountLink.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Kick(Zeze.Builtin.AccountOnline.BAccountLink.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
