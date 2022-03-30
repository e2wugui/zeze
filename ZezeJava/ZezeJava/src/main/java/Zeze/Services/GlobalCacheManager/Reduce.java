package Zeze.Services.GlobalCacheManager;

import Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey;

public class Reduce extends Zeze.Net.Rpc<Param2, Param2> {
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
        Argument = new Param2();
        Result = new Param2();
    }

    public Reduce(GlobalTableKey gkey, int state, long globalSerialId) {
        Argument = new Param2();
        Result = new Param2();
        Argument.GlobalTableKey = gkey;
        Argument.State = state;
        Argument.GlobalSerialId = globalSerialId;
    }
}
