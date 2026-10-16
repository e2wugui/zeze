// auto-generated @formatter:off
package ClientZezex.Linkd;

public class Auth extends Zeze.Net.Rpc<ClientZezex.Linkd.BAuth, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 10000;
    public static final int ProtocolId_ = -997899722; // 3297067574
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42952970027574
    static { register(TypeId_, Auth.class); }

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

    public static final int Success = 0;
    public static final int Error = 1;

    public Auth() {
        Argument = new ClientZezex.Linkd.BAuth();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Auth(ClientZezex.Linkd.BAuth arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
