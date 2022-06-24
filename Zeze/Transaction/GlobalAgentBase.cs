using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Services;

namespace Zeze.Transaction
{
	public abstract class GlobalAgentBase
	{
		private long activeTime = Util.Time.NowUnixMillis;
		public int GlobalCacheManagerHashIndex { get; protected set; }

		public long GetActiveTime()
		{
			return Interlocked.Read(ref activeTime);
		}

		public void SetActiveTime(long value)
		{
			Interlocked.Exchange(ref activeTime, value);
		}

		public AchillesHeelConfig Config { get; private set; }

		public GlobalAgentBase()
		{
			Config = new AchillesHeelConfig(1000, 1000, 10 * 1000);
		}

		public void Initialize(int maxNetPing, int serverProcessTime, int serverReleaseTimeout)
		{
			Config = new AchillesHeelConfig(maxNetPing, serverProcessTime, serverReleaseTimeout);
		}

		public abstract void KeepAlive();

		private volatile Releaser Releasing;

		public enum CheckReleaseResult
		{
			NoRelease,
			Releasing,
			Timeout,
		}

		public bool IsReleasing()
		{
			return Releasing != null;
		}

		public CheckReleaseResult CheckReleaseTimeout(long now, int timeout)
		{
			var tmp = Releasing;
			if (null == tmp)
				return CheckReleaseResult.NoRelease;

			if (tmp.IsCompletedSuccessfully())
			{
				Releasing = null;
				SetActiveTime(Util.Time.NowUnixMillis);
				return CheckReleaseResult.NoRelease;
			}

			return now - tmp.StartTime > timeout ? CheckReleaseResult.Timeout : CheckReleaseResult.Releasing;
		}

		public class Releaser
		{
			public int GlobalIndex;
			public long StartTime = Util.Time.NowUnixMillis;
			public List<Task> Tasks = new();
			public Action EndAction;

			public bool IsCompletedSuccessfully()
			{
				try
				{
					foreach (var task in Tasks)
					{
						if (false == task.IsCompletedSuccessfully)
							return false;
					}
					EndAction?.Invoke();
					return true;
				}
				catch (Exception)
				{
					return false;
				}
			}

			public Releaser(Application zeze, int index, Action endAction)
			{
				EndAction = endAction;
				GlobalIndex = index;
				foreach (var database in zeze.Databases.Values)
				{
					foreach (var table in database.Tables)
					{
						if (!table.IsMemory)
							Tasks.Add(Task.Run(() => table.ReduceInvalidAllLocalOnly(index)));
					}
				}
			}
		}
		// 开始释放本地锁。
		// 1.【要并发，要快】启动线程池来执行，释放锁除了需要和应用互斥，没有其他IO操作，基本上都是cpu。
		// 2. 超时没有释放完成，程序中止。see tryHalt。
		// 3. 每个Global服务一个Releaser.
		public void StartRelease(Application zeze, Action endAction = null)
		{
			lock (this)
            {
				Releasing = new Releaser(zeze, GlobalCacheManagerHashIndex, endAction);
			}
			CancelPending();
		}

		protected abstract void CancelPending();
	}
}
