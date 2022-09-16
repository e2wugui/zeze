package Zeze.Game;

import java.util.concurrent.Future;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServerLoad;

public class LoadReporter {
	private long LastLoginTime;
	private int ReportDelaySeconds;
	private int TimeoutDelaySeconds;
	private Future<?> TimerTask;

	public final Online Online;

	public LoadReporter(Online online) {
		this.Online = online;
	}

	public final void Start() {
		Start(1);
	}

	public final void Start(int delaySeconds) {
		TimeoutDelaySeconds = delaySeconds;
		if (null != TimerTask)
			TimerTask.cancel(false);
		TimerTask = Zeze.Util.Task.scheduleUnsafe(TimeoutDelaySeconds * 1000L, this::OnTimerTask);
	}

	public final void Stop() {
		if (null != TimerTask) {
			TimerTask.cancel(true);
			TimerTask = null;
		}
	}

	private void OnTimerTask() {
		int online = Online.getLocalCount();
		long loginTimes = Online.getLoginTimes();
		int onlineNew = (int)(loginTimes - LastLoginTime);
		LastLoginTime = loginTimes;

		int onlineNewPerSecond = onlineNew / TimeoutDelaySeconds;
		//noinspection ConstantConditions
		var config = Online.providerApp.distribute.loadConfig;
		if (onlineNewPerSecond > config.getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			Report(online, onlineNew);
			// new delay for digestion
			Start(onlineNewPerSecond / config.getMaxOnlineNew() + config.getDigestionDelayExSeconds());
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
		Start();
	}

	public void Report(int online, int onlineNew) {
		var load = new BLoad();
		load.setOnline(online);
		//noinspection ConstantConditions
		load.setProposeMaxOnline(Online.providerApp.distribute.loadConfig.getProposeMaxOnline());
		load.setOnlineNew(onlineNew);
		var bb = ByteBuffer.Allocate(256);
		load.encode(bb);

		var loadServer = new BServerLoad();
		loadServer.ip = Online.providerApp.directIp;
		loadServer.port = Online.providerApp.directPort;
		loadServer.param = new Binary(bb);

		Online.providerApp.zeze.getServiceManagerAgent().setServerLoad(loadServer);
	}
}
