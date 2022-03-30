package Zeze.Services.GlobalCacheManager;

import Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey;

public class Acquire extends Zeze.Net.Rpc<Param, Param2>
{
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Acquire.class.getName());

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
        Result = new Param2();
    }

    public Acquire(GlobalTableKey gkey, int state) {
        Argument = new Param();
        Result = new Param2();
        Argument.GlobalTableKey = gkey;
        Argument.State = state;
    }
}
