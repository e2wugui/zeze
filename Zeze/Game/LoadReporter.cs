using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Serialize;

namespace Zeze.Game
{
	public class LoadReporter
    {
		private long LastLoginTimes;
		private int ReportDelaySeconds;
		private int TimoutDelaySeconds;
		private Util.SchedulerTask Timer;

		public Online Online { get; }

		public LoadReporter(Online online)
        {
			Online = online;
        }

		public void Start(int delaySeconds = 1)
		{
			TimoutDelaySeconds = delaySeconds;
			Timer?.Cancel();
			Timer = Zeze.Util.Scheduler.Schedule(OnTimerTask, TimoutDelaySeconds * 1000);
		}

		public void Stop()
		{
			Timer?.Cancel();
			Timer = null;
		}

		private void OnTimerTask(Zeze.Util.SchedulerTask ThisTask)
		{
			int online = Online.LocalCount;
			long loginTimes = Online.LoginTimes;
			int onlineNew = (int)(loginTimes - LastLoginTimes);
			LastLoginTimes = loginTimes;

			int onlineNewPerSecond = onlineNew / TimoutDelaySeconds;
			var config = Online.ProviderApp.Distribute.LoadConfig;
			if (onlineNewPerSecond > config.MaxOnlineNew)
			{
				// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
				Report(online, onlineNew);
				// new delay for digestion
				Start(onlineNewPerSecond / config.MaxOnlineNew + config.DigestionDelayExSeconds);
				// 消化完后，下一次强迫报告Load。
				ReportDelaySeconds = config.ReportDelaySeconds;
				return;
			}
			// slow report
			ReportDelaySeconds += TimoutDelaySeconds;
			if (ReportDelaySeconds >= config.ReportDelaySeconds)
			{
				ReportDelaySeconds = 0;
				Report(online, onlineNew);
			}
			Start();
		}

		public void Report(int online, int onlineNew)
		{
			var load = new BLoad();
			load.Online = online;
			var config = Online.ProviderApp.Distribute.LoadConfig;
			load.ProposeMaxOnline = config.ProposeMaxOnline;
			load.OnlineNew = onlineNew;
			var bb = ByteBuffer.Allocate(256);
			load.Encode(bb);

			var loadServer = new Zeze.Services.ServiceManager.ServerLoad();
			loadServer.Ip = Online.ProviderApp.DirectIp;
			loadServer.Port = Online.ProviderApp.DirectPort;
			loadServer.Param = new Binary(bb);

			Online.ProviderApp.Zeze.ServiceManagerAgent.SetServerLoad(loadServer);
		}
	}
}
