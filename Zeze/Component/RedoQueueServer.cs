
using System;
using System.Collections.Concurrent;
using Zeze.Builtin.RedoQueue;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Component
{
	public class RedoQueueServer : AbstractRedoQueueServer
	{
		protected override async System.Threading.Tasks.Task<long> ProcessRunTaskRequest(Zeze.Net.Protocol _p)
		{
			var r = _p as RunTask;
			Zeze.Transaction.Transaction.Current.RunWhileCommit(() => r.SendResult());

			var last = await _tQueueLastTaskId.GetOrAddAsync(r.Argument.QueueName);
			r.Result.TaskId = last.TaskId;
			if (r.Argument.PrevTaskId != last.TaskId)
				return Procedure.ErrorRequestId;
			if (!handles.TryGetValue(r.Argument.QueueName, out var queue))
				return Procedure.NotImplement;
			if (!queue.TryGetValue(r.Argument.TaskType, out var handle))
				return Procedure.NotImplement;
			if (!handle(r.Argument.TaskParam))
				return Procedure.LogicError;
			last.TaskId = r.Argument.TaskId;
			r.Result.TaskId = last.TaskId;
			return Procedure.Success;
		}

		private readonly ConcurrentDictionary<string, ConcurrentDictionary<int, Func<Binary, bool>>> handles = new();
		private Server server;

		public RedoQueueServer(Zeze.Application zeze)
		{
			server = new Server(zeze);
			RegisterProtocols(server);
			RegisterZezeTables(zeze);
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
		public void Register(String queue, int type, Func<Binary, bool> task)
		{
			if (!handles.GetOrAdd(queue, (key) => new()).TryAdd(type, task))
				throw new Exception("duplicate task type. " + type);
		}

		public class Server : Zeze.Services.HandshakeServer
		{
			public Server(Zeze.Application zeze)
				: base("RedoQueueServer", zeze)
			{
			}

			public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
			{
				var r = p as RunTask;
				var proc = Zeze.NewProcedure(async () => await factoryHandle.Handle(p), $"RedoQueue={r.Argument.QueueName} RunTask={r.Argument.TaskType}");
				_ = Mission.CallAsync(proc, p, (p, code) => p.SendResultCode(code)); // error result
			}
		}
	}
}
