using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Arch
{
	public abstract class ProviderLoadBase
    {
		private long LastLoginTimes;
		private int ReportDelaySeconds;
		private int TimoutDelaySeconds;
		private SchedulerTask Timer;
		public Application Zeze { get; }

		protected ProviderLoadBase(Application zeze)
        {
			Zeze = zeze;
        }

		public void Start(int delaySeconds = 2)
		{
			TimoutDelaySeconds = delaySeconds;
			Timer?.Cancel();
			Timer = Scheduler.Schedule(OnTimerTask, TimoutDelaySeconds * 1000);
		}

		public void Stop()
		{
			Timer?.Cancel();
			Timer = null;
		}

        public abstract int GetOnlineLocalCount();
        public abstract long GetOnlineLoginTimes();

        public abstract LoadConfig GetLoadConfig();

        public abstract string GetProviderIp();
        public abstract int GetProviderPort();
        
		private void OnTimerTask(SchedulerTask ThisTask)
		{
            var overload = BLoad.eWorkFine; // TODO 过载检测。
            
			int online = GetOnlineLocalCount();
			long loginTimes = GetOnlineLoginTimes();
			int onlineNew = (int)(loginTimes - LastLoginTimes);
			LastLoginTimes = loginTimes;

			int onlineNewPerSecond = onlineNew / TimoutDelaySeconds;
			var config = GetLoadConfig();

            if (overload != BLoad.eWorkFine)
            {
                // fast report
                Report(overload, online, onlineNew);
                Start(2);
                return;
            }
            
			if (onlineNewPerSecond > config.MaxOnlineNew)
			{
				// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
				Report(overload, online, onlineNew);
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
				Report(overload, online, onlineNew);
			}
			Start();
		}

		public void Report(int overload, int online, int onlineNew)
		{
			var load = new BLoad();
			load.Overload = overload;
			load.Online = online;
			var config = GetLoadConfig();
			load.ProposeMaxOnline = config.ProposeMaxOnline;
			load.OnlineNew = onlineNew;
			var bb = ByteBuffer.Allocate(256);
			load.Encode(bb);

			var loadServer = new Services.ServiceManager.ServerLoad();
			loadServer.Ip = GetProviderIp();
			loadServer.Port = GetProviderPort();
			loadServer.Param = new Binary(bb);

			Zeze.ServiceManager.SetServerLoad(loadServer);
		}
	}
}
