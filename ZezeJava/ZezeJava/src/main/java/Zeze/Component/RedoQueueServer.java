package Zeze.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;

public class RedoQueueServer extends AbstractRedoQueueServer {
	private final ConcurrentHashMap<String, LongConcurrentHashMap<Predicate<Binary>>> handles = new ConcurrentHashMap<>();
	private final Server server;

	public RedoQueueServer(Zeze.Application zeze) {
		server = new Server(zeze);
		RegisterProtocols(server);
		RegisterZezeTables(zeze);
	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(server);
		UnRegisterZezeTables(server.getZeze());
	}

	public void start() throws Exception {
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}

	/**
	 * 注册任务，
	 */
	public void register(String queue, int type, Predicate<Binary> task) {
		if (null != handles.computeIfAbsent(queue, (key) -> new LongConcurrentHashMap<>()).putIfAbsent(type, task))
			throw new IllegalStateException("duplicate task type. " + type);
	}

	@Override
	protected long ProcessRunTaskRequest(Zeze.Builtin.RedoQueue.RunTask r) {
		var last = _tQueueLastTaskId.getOrAdd(r.Argument.getQueueName());
		r.Result.setTaskId(last.getTaskId());
		if (r.Argument.getPrevTaskId() != last.getTaskId())
			return Procedure.ErrorRequestId;
		var queue = handles.get(r.Argument.getQueueName());
		if (queue == null)
			return Procedure.NotImplement;
		var handle = queue.get(r.Argument.getTaskType());
		if (handle == null)
			return Procedure.NotImplement;
		if (!handle.test(r.Argument.getTaskParam()))
			return Procedure.LogicError;
		last.setTaskId(r.Argument.getTaskId());
		r.Result.setTaskId(last.getTaskId());
		return Procedure.Success;
	}

	public static class Server extends Zeze.Services.HandshakeServer {
		public Server(Zeze.Application zeze) {
			super("RedoQueueServer", zeze);
		}

		@Override
		public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
			Task.run(getZeze().newProcedure(() -> {
						Transaction.whileCommit(() -> p.SendResultCode(p.getResultCode()));
						return factoryHandle.Handle.handle(p);
					}, p.getClass().getName(), TransactionLevel.Serializable, p.getUserState()), p,
					Protocol::trySendResultCode, DispatchMode.Normal);
		}
	}
}
