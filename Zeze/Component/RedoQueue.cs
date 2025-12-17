using RocksDbSharp;
using System;
using System.Collections.Concurrent;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.RedoQueue;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;
using DotNext.Threading;
using System.Threading;

namespace Zeze.Component
{

	/*
	 * 连接：
	 * 1.每个队列一个连接服务。
	 * 2.可以从可用的zeze-server中选择部分，配置到zeze.xml中。
	 * 3.【可选】使用ServiceManager动态发现zeze-server。感觉没有必要。
	 */
	public class RedoQueue : Zeze.Services.HandshakeClient
	{
		private readonly AsyncLock Mutex = AsyncLock.Exclusive();

		public RedoQueue(string name, Zeze.Config config)
			: base(name, config)
		{
		}

		public override async void Start()
		{
			using (await Mutex.AcquireAsync(CancellationToken.None))
			{
				if (null != Db)
					return;

				var dbOptions = new DbOptions().SetCreateIfMissing(true);
				var dbHome = Name;
				var columnFamilies = new ColumnFamilies();
				foreach (var cf in RocksDb.ListColumnFamilies(dbOptions, dbHome))
				{
					columnFamilies.Add(cf, CfOptions);
				}
				// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
				Db = await AsyncRocksDb.OpenAsync(dbOptions, dbHome, columnFamilies, AsyncExecutor.Instance);
				foreach (var f in columnFamilies)
				{
					Families[f.Name] = new(Db.RocksDb.GetColumnFamily(f.Name));
				}
				FamilyLastDoneTaskId = await GetOrAddFamily("FamilyLastDoneTaskId");
				FamilyTaskQueue = await GetOrAddFamily("FamilyTaskQueue");
				using (var qit = Db.RocksDb.NewIterator(FamilyTaskQueue))
				{
					qit.SeekToLast();
					if (qit.Valid())
					{
						var last = ByteBuffer.Wrap(qit.Key());
						LastTaskId = last.ReadLong();
					}
				}
				var done = await Db.GetAsync(LastDoneTaskIdKey, LastDoneTaskIdKey.Length, FamilyLastDoneTaskId);
				if (done != null)
				{
					LastDoneTaskId = ByteBuffer.Wrap(done).ReadLong();
				}
				base.Start();

			}
		}

		public override async void Stop()
		{
			base.Stop();
			using (await Mutex.AcquireAsync(CancellationToken.None))
			{

				Db.RocksDb.Dispose();
				Db = null;
			}
		}

		public async Task AddAsync(int taskType, Zeze.Serialize.Serializable taskParam)
		{
			using (await Mutex.AcquireAsync(CancellationToken.None))
			{
				var key = ByteBuffer.Allocate(16);
				++LastTaskId;
				key.WriteLong(LastTaskId);

				var task = new BQueueTask();
				task.QueueName = Name;
				task.PrevTaskId = LastTaskId - 1;
				task.TaskId = LastTaskId;
				task.TaskType = taskType;
				var value = ByteBuffer.Allocate(1024 + 16);
				task.Encode(value);

				// 保存完整的rpc请求，重新发送的时候不用再次打包。
				await Db.PutAsync(key.Bytes, key.Size, value.Bytes, value.Size, FamilyTaskQueue, WriteOptions);
				await TryStartSendNextTask(task, null);
			}
		}

		private RunTask Pending;
		private AsyncSocket Socket;

		private async Task TryStartSendNextTask(BQueueTask add, AsyncSocket socket)
		{
			if (null != Pending)
				return;

			if (LastDoneTaskId < LastTaskId) {
				var taskId = LastDoneTaskId + 1;
				var rpc = new RunTask();
				if (add != null && taskId == add.TaskId) {
					rpc.Argument = add; // 最近加入的就是要发送的。优化！
				} else {
					// 最近加入的不是要发送的，从Db中读取。
					var key = ByteBuffer.Allocate(16);
					key.WriteLong(taskId);
					var value = await Db.GetAsync(key.Bytes, key.Size, FamilyTaskQueue, ReadOptions);
					if (null == value)
						return; // error
					rpc.Argument.Decode(ByteBuffer.Wrap(value));
				}
				if (null == Socket) {
					Socket = socket;
					if (null == Socket) {
						Socket = GetSocket();
						if (null == Socket)
							return;
					}
				}
				if (rpc.Send(Socket, ProcessRunTaskResult))
					Pending = rpc;
			}
		}

		private async Task<long> ProcessRunTaskResult(Protocol p)
		{
			var rpc = p as RunTask;
			if (Pending != rpc)
				return ResultCode.LogicError;

			Pending = null;
			if (rpc.ResultCode == 0L || rpc.ResultCode == ResultCode.ErrorRequestId)
			{
				LastDoneTaskId = rpc.Result.TaskId;
				var value = ByteBuffer.Allocate(16);
				value.WriteLong(LastDoneTaskId);
				await Db.PutAsync(LastDoneTaskIdKey, LastDoneTaskIdKey.Length, value.Bytes, value.Size, FamilyLastDoneTaskId, WriteOptions);
				await TryStartSendNextTask(null, rpc.Sender);
				return 0L;
			}

			return rpc.ResultCode;
		}

		public override async void OnHandshakeDone(AsyncSocket sender)
		{
			base.OnHandshakeDone(sender);
			using (await Mutex.AcquireAsync(CancellationToken.None))
			{
				await TryStartSendNextTask(null, sender);
			}
		}

		public override async void OnSocketClose(AsyncSocket socket, Exception ex)
		{
			base.OnSocketClose(socket, ex);
			using (await Mutex.AcquireAsync(CancellationToken.None))
			{ 
				if (Socket == socket) {
					Socket = null;
				}
			}
		}

		public class ColumnFamilyAsync
        {
			public async Task TryCreateAsync(RedoQueue rq, string name)
            {
				using (await rq.Mutex.AcquireAsync(CancellationToken.None))
                {
					if (Handle == null)
					Handle = await rq.Db.CreateColumnFamily(rq.CfOptions, name);
				}
			}
			public ColumnFamilyHandle Handle { get; set; }

			public ColumnFamilyAsync(ColumnFamilyHandle handle = null)
            {
				Handle = handle;
            }
		}

		private AsyncRocksDb Db;
		private readonly ColumnFamilyOptions CfOptions = new ColumnFamilyOptions();
		public WriteOptions WriteOptions = new WriteOptions();
		public ReadOptions ReadOptions = new ReadOptions();
		private readonly ConcurrentDictionary<string, ColumnFamilyAsync> Families = new();
		private ColumnFamilyHandle FamilyLastDoneTaskId;
		private ColumnFamilyHandle FamilyTaskQueue;
		private long LastTaskId;
		private long LastDoneTaskId;
		private byte[] LastDoneTaskIdKey = Encoding.UTF8.GetBytes("LastDoneTaskId");

		async Task<ColumnFamilyHandle> GetOrAddFamily(string name)
		{
			var cf = Families.GetOrAdd(name, (key) => new ColumnFamilyAsync());
			await cf.TryCreateAsync(this, name);
			return cf.Handle;
		}
	}
}
