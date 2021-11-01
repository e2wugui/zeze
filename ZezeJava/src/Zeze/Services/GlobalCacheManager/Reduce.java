package Zeze.Services.GlobalCacheManager;

public class Reduce extends Zeze.Net.Rpc<Param, Param> {
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Reduce.class.getName());

    @Override
    public int getModuleId() {
        return 0;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Reduce() {
        Argument = new Param();
        Result = new Param();
    }

    public Reduce(GlobalTableKey gkey, int state) {
        Argument = new Param();
        Result = new Param();
        Argument.GlobalTableKey = gkey;
        Argument.State = state;
    }
}
