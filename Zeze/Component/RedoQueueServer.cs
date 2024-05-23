using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Builtin.RedoQueue;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Component
{
	public class RedoQueueServer : AbstractRedoQueueServer
	{
		protected override async Task<long> ProcessRunTaskRequest(Protocol _p)
		{
			var r = _p as RunTask;
			Transaction.Transaction.Current.RunWhileCommit(() => r.SendResult());

			var last = await _tQueueLastTaskId.GetOrAddAsync(r.Argument.QueueName);
			r.Result.TaskId = last.TaskId;
			if (r.Argument.PrevTaskId != last.TaskId)
				return ResultCode.ErrorRequestId;
			if (!handles.TryGetValue(r.Argument.QueueName, out var queue))
				return ResultCode.NotImplement;
			if (!queue.TryGetValue(r.Argument.TaskType, out var handle))
				return ResultCode.NotImplement;
			if (!handle(r.Argument.TaskParam))
				return ResultCode.LogicError;
			last.TaskId = r.Argument.TaskId;
			r.Result.TaskId = last.TaskId;
			return ResultCode.Success;
		}

		private readonly ConcurrentDictionary<string, ConcurrentDictionary<int, Func<Binary, bool>>> handles = new();
		private Server server;

		public RedoQueueServer(Application zeze)
		{
			server = new Server(zeze);
			RegisterProtocols(server);
			RegisterZezeTables(zeze);
		}

		public override void UnRegister()
		{
			UnRegisterProtocols(server);
			UnRegisterZezeTables(server.Zeze);
		}

		public void Start()
		{
			server.Start();
		}

		public void Stop()
		{
			server.Stop();
		}

		/**
		 * ×¢²áÈÎÎñ£¬
		 */
		public void Register(string queue, int type, Func<Binary, bool> task)
		{
			if (!handles.GetOrAdd(queue, _ => new()).TryAdd(type, task))
				throw new Exception("duplicate task type. " + type);
		}

		public class Server : Services.HandshakeServer
		{
			public Server(Application zeze)
				: base("RedoQueueServer", zeze)
			{
			}

			public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
			{
				var r = p as RunTask;
				var proc = Zeze.NewProcedure(async () => await factoryHandle.Handle(p),
					$"RedoQueue={r.Argument.QueueName} RunTask={r.Argument.TaskType}");
				_ = Mission.CallAsync(proc, p, (p, code) => p.TrySendResultCode(code)); // error result
			}
		}
	}
}
