// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// 通知linkd订阅模块的服务列表。
public class Subscribe extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BSubscribe, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 1110460218;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47280110454586

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

    public Subscribe() {
        Argument = new Zeze.Builtin.Provider.BSubscribe();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Subscribe(Zeze.Builtin.Provider.BSubscribe arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
