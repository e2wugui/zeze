package Zeze.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Services.HandshakeServer;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;

public class RedoQueueServer extends AbstractRedoQueueServer {
	private final ConcurrentHashMap<String, LongConcurrentHashMap<Predicate<Binary>>> handles = new ConcurrentHashMap<>();
	private final Server server;

	public RedoQueueServer(Application zeze) {
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
		if (null != handles.computeIfAbsent(queue, __ -> new LongConcurrentHashMap<>()).putIfAbsent(type, task))
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

	public static class Server extends HandshakeServer {
		public Server(Application zeze) {
			super("RedoQueueServer", zeze);
		}
		// 不覆写dispatchProtocol（CP1-F2）：基类对事务级协议copy网络buffer后
		// 在procedure内重解码（Service.dispatchProtocol(long,ByteBuffer,...)），redo安全且
		// 不异步引用可回收的网络缓冲；RunTask的factoryHandle.Level默认Serializable，
		// 事务语义与原覆写一致。原覆写在procedure内直接设bb.ReadIndex=0重解码网络buffer，
		// 派发入池后缓冲可能被回收复用，解码出错误协议内容。
	}
}
