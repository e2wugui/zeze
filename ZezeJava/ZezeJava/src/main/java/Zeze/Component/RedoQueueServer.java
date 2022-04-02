package Zeze.Component;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Func1;
import Zeze.Util.Task;

public class RedoQueueServer extends AbstractRedoQueue {
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Func1<Binary, Boolean>>> handles = new ConcurrentHashMap<>();
    private Server server;

    public RedoQueueServer(Zeze.Application zeze) throws Throwable {
        server = new Server(zeze);
        RegisterProtocols(server);
        RegisterZezeTables(zeze);
    }

    public void Start() throws Throwable {
        server.Start();
    }

    public void Stop() throws Throwable {
        server.Stop();
    }

    /**
     * 注册任务，
     * @param type
     * @param task
     */
    public void register(String queue, int type, Func1<Binary, Boolean> task) {
        if (null != handles.computeIfAbsent(queue, (key) -> new ConcurrentHashMap<>()).putIfAbsent(type, task))
            throw new RuntimeException("duplicate task type. " + type);
    }

    @Override
    protected long ProcessRunTaskRequest(Zeze.Beans.RedoQueue.RunTask r) throws Throwable {
        var last = _tQueueLastTaskId.getOrAdd(r.Argument.getQueueName());
        r.Result.setTaskId(last.getTaskId());
        if (r.Argument.getPrevTaskId() != last.getTaskId())
            return Procedure.ErrorRequestId;
        var queue = handles.get(r.Argument.getQueueName());
        if (null == queue)
            return Procedure.NotImplement;
        var handle = queue.get(r.Argument.getTaskType());
        if (null == handle)
            return Procedure.NotImplement;
        if (!handle.call(r.Argument.getTaskParam()))
            return Procedure.LogicError;
        last.setTaskId(r.Argument.getTaskId());
        r.Result.setTaskId(last.getTaskId());
        return Procedure.Success;
    }

    public static class Server extends Zeze.Services.HandshakeServer {
        public Server(Zeze.Application zeze) throws Throwable {
            super("RedoQueueServer", zeze);
        }

        @Override
        public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
            var proc = getZeze().NewProcedure(() -> factoryHandle.Handle.handle(p),
                    p.getClass().getName(), TransactionLevel.Serializable, p.getUserState());
            proc.RunWhileCommit = () -> p.SendResultCode(p.getResultCode());
            Task.run(proc, p, (_p, code) -> _p.SendResultCode(code));
        }
    }
}
