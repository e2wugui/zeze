package Zeze.Services.GlobalCacheManager;

public class NormalClose extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(NormalClose.class.getName());

    @Override
    public int getModuleId() {
        return 0;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public NormalClose() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Zeze.Transaction.EmptyBean();
    }
}