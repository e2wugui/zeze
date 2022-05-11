
using System.Collections.Concurrent;
using System.Text;
using Zeze.Util;

namespace Zeze.Services.GlobalCacheManager
{
	public class GlobalCacheManagerPerf
	{
		private const int ACQUIRE_STATE_COUNT = 3;
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private readonly Util.AtomicLong serialIdGenerator;
		private long lastSerialId;

		private readonly ConcurrentDictionary<Acquire, long> acquires = new();
		private readonly ConcurrentDictionary<Reduce, long> reduces = new();

		private readonly AtomicLong[] totalAcquireCounts = new AtomicLong[ACQUIRE_STATE_COUNT];
		private readonly AtomicLong[] totalAcquireTimes = new AtomicLong[ACQUIRE_STATE_COUNT];
		private readonly AtomicLong[] maxAcquireTimes = new AtomicLong[ACQUIRE_STATE_COUNT];
		private readonly AtomicLong totalReduceCount = new();
		private readonly AtomicLong totalReduceTime = new();
		private readonly AtomicLong maxReduceTime = new();

		public GlobalCacheManagerPerf(Util.AtomicLong serialIdGenerator)
		{
			this.serialIdGenerator = serialIdGenerator;
			lastSerialId = serialIdGenerator.Get();
			for (int i = 0; i < ACQUIRE_STATE_COUNT; i++)
			{
				totalAcquireCounts[i] = new AtomicLong();
				totalAcquireTimes[i] = new AtomicLong();
				maxAcquireTimes[i] = new AtomicLong();
			}
			Scheduler.Schedule(Report, 1000, 1000);
		}

		public void OnAcquireBegin(Acquire rpc)
		{
			if ((rpc.Argument.State & 0xffff_ffffL) < ACQUIRE_STATE_COUNT)
				acquires[rpc] = Time.NanoTime();
		}

		public void OnAcquireEnd(Acquire rpc)
		{
			if (acquires.TryRemove(rpc, out var beginTime))
			{
				var time = Time.NanoTime() - beginTime;
				totalAcquireCounts[rpc.Argument.State].IncrementAndGet();
				totalAcquireTimes[rpc.Argument.State].AddAndGet(time);
				var maxAcquireTime = maxAcquireTimes[rpc.Argument.State];
				long maxTime;
				do
					maxTime = maxAcquireTime.Get();
				while (time > maxTime && !maxAcquireTime.CompareAndSet(maxTime, time));
			}
		}

		public void OnReduceBegin(Reduce rpc)
		{
			reduces[rpc] = Time.NanoTime();
		}

		public void OnReduceCancel(Reduce rpc)
		{
			reduces.TryRemove(rpc, out _);
		}

		public void OnReduceEnd(Reduce rpc)
		{
			if (reduces.TryRemove(rpc, out var beginTime))
			{
				var time = Time.NanoTime() - beginTime;
				totalReduceCount.IncrementAndGet();
				totalReduceTime.AddAndGet(time);
				long maxTime;
				do
					maxTime = maxReduceTime.Get();
				while (time > maxTime && !maxReduceTime.CompareAndSet(maxTime, time));
			}
		}

		private void Report(SchedulerTask ThisTask)
		{
			long curSerialId = serialIdGenerator.Get();
			long serialIds = curSerialId - lastSerialId;
			lastSerialId = curSerialId;

			if ((serialIds | totalReduceCount.Get() | (long)acquires.Count | (long)reduces.Count) == 0)
			{
				int i = 0;
				for (; i < ACQUIRE_STATE_COUNT; i++)
				{
					if (totalAcquireCounts[i].Get() != 0)
						break;
				}
				if (i == ACQUIRE_STATE_COUNT)
					return;
			}

			var sb = new StringBuilder().Append("SerialIds = ").Append(serialIds).Append('\n');
			for (int i = 0; i < ACQUIRE_STATE_COUNT; i++)
			{
				long c = totalAcquireCounts[i].Get();
				sb.Append("Acquires[").Append(i).Append("] = ").Append(c);
				if (c > 0)
				{
					sb.Append(", ").Append(totalAcquireTimes[i].Get() / c / 1_000).Append(" us/acquire, max: ")
							.Append(maxAcquireTimes[i].Get() / 1_000_000).Append(" ms");
				}
				sb.Append('\n');
			}
			long count = totalReduceCount.Get();
			sb.Append("Reduces = ").Append(count);
			if (count > 0)
			{
				sb.Append(", ").Append(totalReduceTime.Get() / count / 1_000).Append(" us/reduce, max: ")
						.Append(maxReduceTime.Get() / 1_000_000).Append(" ms");
			}
			sb.Append("\nAcquire/Reduce Pendings = ").Append(acquires.Count).Append(" / ")
					.Append(reduces.Count).Append('\n');
			logger.Info($"\n{sb}");

			for (int i = 0; i < ACQUIRE_STATE_COUNT; i++)
			{
				totalAcquireCounts[i].GetAndSet(0);
				totalAcquireTimes[i].GetAndSet(0);
				maxAcquireTimes[i].GetAndSet(0);
			}
			totalReduceCount.GetAndSet(0);
			totalReduceTime.GetAndSet(0);
			maxReduceTime.GetAndSet(0);
		}
	}
}
