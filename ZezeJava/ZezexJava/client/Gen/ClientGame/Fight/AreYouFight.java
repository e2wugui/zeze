// auto-generated @formatter:off
package ClientGame.Fight;

public class AreYouFight extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 4;
    public static final int ProtocolId_ = 500009000;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 17679878184
    static { register(TypeId_, AreYouFight.class); }

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

    public AreYouFight() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
