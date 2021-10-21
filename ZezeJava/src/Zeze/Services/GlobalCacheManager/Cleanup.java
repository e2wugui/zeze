package Zeze.Services.GlobalCacheManager;

public class Cleanup extends Zeze.Net.Rpc<AchillesHeel, Zeze.Transaction.EmptyBean> {
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Cleanup.class.getName());

    @Override
    public int getModuleId() {
        return 0;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Cleanup() {
        Argument = new AchillesHeel();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
