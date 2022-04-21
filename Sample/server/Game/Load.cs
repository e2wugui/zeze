using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Serialize;

namespace Game
{
	/// <summary>
	/// 定时向所有的 linkd 报告负载。
	/// 如果启用cahce-sync，可能linkd数量比较多。所以正常情况下，报告间隔应长点。比如10秒。
	/// </summary>
	public class Load
    {
		public Zeze.Util.AtomicLong LoginCount { get; } = new Zeze.Util.AtomicLong();
		public Zeze.Util.AtomicLong LogoutCount { get; } = new Zeze.Util.AtomicLong();

		private long LoginCountLast;
		private int ReportDelaySeconds;
		private int TimoutDelaySeconds;

		public void StartTimerTask(int delaySeconds = 1)
		{
			TimoutDelaySeconds = delaySeconds;
			Zeze.Util.Scheduler.Schedule(OnTimerTask, TimoutDelaySeconds * 1000);
		}

		private void OnTimerTask(Zeze.Util.SchedulerTask ThisTask)
		{
			long login = LoginCount.Get();
			long logout = LogoutCount.Get();
			int online = (int)(login - logout);
			int onlineNew = (int)(login - LoginCountLast);
			LoginCountLast = login;

			int onlineNewPerSecond = onlineNew / TimoutDelaySeconds;
			if (onlineNewPerSecond > App.Instance.Config.MaxOnlineNew)
			{
				// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
				Report(online, onlineNew);
				// new delay for digestion
				StartTimerTask(onlineNewPerSecond / App.Instance.Config.MaxOnlineNew + App.Instance.Config.DigestionDelayExSeconds);
				// 消化完后，下一次强迫报告Load。
				ReportDelaySeconds = App.Instance.Config.ReportDelaySeconds;
				return;
			}
			// slow report
			ReportDelaySeconds += TimoutDelaySeconds;
			if (ReportDelaySeconds >= App.Instance.Config.ReportDelaySeconds)
			{
				ReportDelaySeconds = 0;
				Report(online, onlineNew);
			}
			StartTimerTask();
		}

		public void Report(int online, int onlineNew)
		{
			var load = new BLoad();
			load.Online = online;
			load.ProposeMaxOnline = App.Instance.Config.ProposeMaxOnline;
			load.OnlineNew = onlineNew;
			var bb = ByteBuffer.Allocate(256);
			load.Encode(bb);

			var loadServer = new Zeze.Services.ServiceManager.ServerLoad();
			loadServer.Ip = App.Instance.ProviderApp.DirectIp;
			loadServer.Port = App.Instance.ProviderApp.DirectPort;
			loadServer.Param = new Binary(bb);

			App.Instance.ProviderApp.Zeze.ServiceManagerAgent.SetServerLoad(loadServer);
		}
	}
}
