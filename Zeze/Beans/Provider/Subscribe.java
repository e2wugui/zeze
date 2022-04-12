// auto-generated @formatter:off
package Zeze.Beans.Provider;

public class Subscribe extends Zeze.Net.Rpc<Zeze.Beans.Provider.BSubscribe, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -629827684;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Subscribe() {
        Argument = new Zeze.Beans.Provider.BSubscribe();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
