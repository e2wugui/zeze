package Zeze.MQ.Master;

import Zeze.Builtin.MQ.Master.Subscribe;

public class Master extends AbstractMaster {
    @Override
    protected long ProcessOpenMQRequest(Zeze.Builtin.MQ.Master.OpenMQ r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSubscribeRequest(Subscribe r) throws Exception {
        return 0;
    }
}
