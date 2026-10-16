// auto-generated @formatter:off
package ClientZezex.Linkd;

public class Sc extends Zeze.Net.Protocol<ClientZezex.Linkd.BAuth> {
    public static final int ModuleId_ = 10000;
    public static final int ProtocolId_ = -1005954929; // 3289012367
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42952961972367
    static { register(TypeId_, Sc.class); }

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

    public Sc() {
        Argument = new ClientZezex.Linkd.BAuth();
    }

    public Sc(ClientZezex.Linkd.BAuth arg) {
        Argument = arg;
    }
}
