// auto-generated @formatter:off
package ClientZezex.Linkd;

public class Cs extends Zeze.Net.Protocol<ClientZezex.Linkd.BAuth> {
    public static final int ModuleId_ = 10000;
    public static final int ProtocolId_ = 961007421;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42950633967421
    static { register(TypeId_, Cs.class); }

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

    public Cs() {
        Argument = new ClientZezex.Linkd.BAuth();
    }

    public Cs(ClientZezex.Linkd.BAuth arg) {
        Argument = arg;
    }
}
