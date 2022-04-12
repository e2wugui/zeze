package Zeze.Component;

public class RedoQueueServer extends AbstractRedoQueueServer {
    @Override
    protected long ProcessRunTaskRequest(Zeze.Beans.RedoQueue.RunTask r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
