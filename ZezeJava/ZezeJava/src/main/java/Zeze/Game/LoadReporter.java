package Zeze.Game;

import java.util.concurrent.Future;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

public class LoadReporter {
	private long LastLoginTime;
	private int ReportDelaySeconds;
	private int TimeoutDelaySeconds;
	private Future<?> TimerTask;

	public Online Online;

	public LoadReporter(Online online) {
		this.Online = online;
	}

	public final void StartTimerTask() {
		StartTimerTask(1);
	}

	public final void StartTimerTask(int delaySeconds) {
		TimeoutDelaySeconds = delaySeconds;
		if (null != TimerTask)
			TimerTask.cancel(false);
		TimerTask = Zeze.Util.Task.schedule(TimeoutDelaySeconds * 1000L, this::OnTimerTask);
	}

	private void OnTimerTask() {
		int online = Online.getLocalCount();
		long loginTimes = Online.getLoginTimes();
		int onlineNew = (int)(loginTimes - LastLoginTime);
		LastLoginTime = loginTimes;

		int onlineNewPerSecond = onlineNew / TimeoutDelaySeconds;
		var config = Online.ProviderApp.Distribute.LoadConfig;
		if (onlineNewPerSecond > config.getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			Report(online, onlineNew);
			// new delay for digestion
			StartTimerTask(onlineNewPerSecond / config.getMaxOnlineNew() + config.getDigestionDelayExSeconds());
			// 消化完后，下一次强迫报告Load。
			ReportDelaySeconds = config.getReportDelaySeconds();
			return;
		}
		// slow report
		ReportDelaySeconds += TimeoutDelaySeconds;
		if (ReportDelaySeconds >= config.getReportDelaySeconds()) {
			ReportDelaySeconds = 0;
			Report(online, onlineNew);
		}
		StartTimerTask();
	}

	public void Report(int online, int onlineNew) {
		var load = new BLoad();
		load.setOnline(online);
		load.setProposeMaxOnline(Online.ProviderApp.Distribute.LoadConfig.getProposeMaxOnline());
		load.setOnlineNew(onlineNew);
		var bb = ByteBuffer.Allocate(256);
		load.Encode(bb);

		var loadServer = new Zeze.Services.ServiceManager.ServerLoad();
		loadServer.Ip = Online.ProviderApp.DirectIp;
		loadServer.Port = Online.ProviderApp.DirectPort;
		loadServer.Param = new Binary(bb);

		try {
			Online.ProviderApp.Zeze.getServiceManagerAgent().SetServerLoad(loadServer);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
