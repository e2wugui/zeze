package Zeze.Services;

public class LoginQueueClient extends AbstractLoginQueueClient {
    @Override
    protected long ProcessPutQueuePosition(Zeze.Builtin.LoginQueue.PutQueuePosition p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessPutLoginToken(Zeze.Builtin.LoginQueue.PutLoginToken p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessPutQueueFull(Zeze.Builtin.LoginQueue.PutQueueFull p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
