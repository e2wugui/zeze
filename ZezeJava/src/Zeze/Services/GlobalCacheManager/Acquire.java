package Zeze.Services.GlobalCacheManager;

public class Acquire extends Zeze.Net.Rpc<Param, Param>
{
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Acquire.class.getName());

    @Override
    public int getModuleId() {
        return 0;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Acquire() {
        Argument = new Param();
        Result = new Param();
    }

    public Acquire(GlobalTableKey gkey, int state) {
        Argument = new Param();
        Result = new Param();
        Argument.GlobalTableKey = gkey;
        Argument.State = state;
    }
}
