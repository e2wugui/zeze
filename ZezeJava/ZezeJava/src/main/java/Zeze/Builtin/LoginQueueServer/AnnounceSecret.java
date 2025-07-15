// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

public class AnnounceSecret extends Zeze.Net.Rpc<Zeze.Builtin.LoginQueueServer.BSecret.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11042;
    public static final int ProtocolId_ = 829648969;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47425858531401
    static { register(TypeId_, AnnounceSecret.class); }

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

    public AnnounceSecret() {
        Argument = new Zeze.Builtin.LoginQueueServer.BSecret.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public AnnounceSecret(Zeze.Builtin.LoginQueueServer.BSecret.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
