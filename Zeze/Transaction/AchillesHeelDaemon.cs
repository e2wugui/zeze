using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Services;

namespace Zeze.Transaction
{
	/// <summary>
	/// 说明详见：zeze/ZezeJava/.../AchillesHeelDaemon.java
	/// </summary>
	/// <typeparam name="T"></typeparam>
	public class AchillesHeelDaemon
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
		private readonly Application Zeze;
		private readonly GlobalAgentBase[] Agents;
		private readonly Thread Thread;

		public AchillesHeelConfig GetConfig(int index)
		{
			return Agents[index].Config;
		}

		public AchillesHeelDaemon(Application zeze, GlobalAgentBase[] agents)
		{
			Zeze = zeze;
			Agents = new GlobalAgentBase[agents.Length];
			for (int i = 0; i < agents.Length; ++i)
				Agents[i] = agents[i];
			Thread = new Thread(Run);
		}

		public void Start()
        {
			Thread.Start();
        }

		private volatile bool Running = true;

		public void StopAndJoin()
		{
			Running = false;
			Thread.Join();
		}
		public void Run()
		{
			try
			{
				lock (this)
				{
					while (Running)
					{
						var now = Util.Time.NowUnixMillis;
						for (int i = 0; i < Agents.Length; ++i)
						{
							var agent = Agents[i];
							var config = agent.Config;
							if (null == config)
								continue; // skip agent not login

							var rr = agent.CheckReleaseTimeout(now, config.ServerReleaseTimeout);
							if (rr == GlobalAgentBase.CheckReleaseResult.Timeout)
							{
								logger.Fatal("AchillesHeelDaemon global release timeout. index=" + i);
								Process.GetCurrentProcess().Kill();
							}

							var idle = now - agent.GetActiveTime();
							if (idle > config.ServerKeepAliveIdleTimeout)
							{
								//logger.Debug($"KeepAlive ServerKeepAliveIdleTimeout={config.ServerKeepAliveIdleTimeout}");
								agent.KeepAlive();
							}

							if (idle > config.ServerDaemonTimeout)
							{
								if (rr != GlobalAgentBase.CheckReleaseResult.Releasing)
                                {
									// 这个判断只能避免正在Releasing时不要启动新的Release。
									// 如果Global一直恢复不了，那么每ServerDaemonTimeout会再次尝试Release，
									// 这里没法快速手段判断本Server是否存在从该Global获取的记录锁。
									// 在Agent中增加获得的计数是个方案，但挺烦的。
									logger.Warn($"StartRelease ServerDaemonTimeout {config.ServerReleaseTimeout}");
									agent.StartRelease(Zeze);
								}
							}
						}
						Monitor.Wait(this, 1000);
					}
				}
			}
			catch (Exception ex)
			{
				// 这个线程不准出错。
				logger.Fatal(ex, "AchillesHeelDaemon ");
				Process.GetCurrentProcess().Kill();
			}
		}
	}
}
