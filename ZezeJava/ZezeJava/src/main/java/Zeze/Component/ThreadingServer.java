package Zeze.Component;

public class ThreadingServer extends AbstractThreadingServer {
    @Override
    protected long ProcessMutexTryLockRequest(Zeze.Builtin.Threading.MutexTryLock r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessMutexUnlockRequest(Zeze.Builtin.Threading.MutexUnlock r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
