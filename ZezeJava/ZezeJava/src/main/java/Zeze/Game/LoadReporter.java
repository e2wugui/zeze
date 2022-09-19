package Zeze.Game;

import java.util.concurrent.Future;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServerLoad;

public class LoadReporter {
	private long lastLoginTime;
	private int reportDelaySeconds;
	private int timeoutDelaySeconds;
	private Future<?> timerTask;

	public final Online online;

	public LoadReporter(Online online) {
		this.online = online;
	}

	public final void start() {
		start(1);
	}

	public final void start(int delaySeconds) {
		timeoutDelaySeconds = delaySeconds;
		if (null != timerTask)
			timerTask.cancel(false);
		timerTask = Zeze.Util.Task.scheduleUnsafe(timeoutDelaySeconds * 1000L, this::onTimerTask);
	}

	public final void stop() {
		if (null != timerTask) {
			timerTask.cancel(true);
			timerTask = null;
		}
	}

	private void onTimerTask() {
		int online = this.online.getLocalCount();
		long loginTimes = this.online.getLoginTimes();
		int onlineNew = (int)(loginTimes - lastLoginTime);
		lastLoginTime = loginTimes;

		int onlineNewPerSecond = onlineNew / timeoutDelaySeconds;
		//noinspection ConstantConditions
		var config = this.online.providerApp.distribute.loadConfig;
		if (onlineNewPerSecond > config.getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			report(online, onlineNew);
			// new delay for digestion
			start(onlineNewPerSecond / config.getMaxOnlineNew() + config.getDigestionDelayExSeconds());
			// 消化完后，下一次强迫报告Load。
			reportDelaySeconds = config.getReportDelaySeconds();
			return;
		}
		// slow report
		reportDelaySeconds += timeoutDelaySeconds;
		if (reportDelaySeconds >= config.getReportDelaySeconds()) {
			reportDelaySeconds = 0;
			report(online, onlineNew);
		}
		start();
	}

	public void report(int online, int onlineNew) {
		var load = new BLoad();
		load.setOnline(online);
		//noinspection ConstantConditions
		load.setProposeMaxOnline(this.online.providerApp.distribute.loadConfig.getProposeMaxOnline());
		load.setOnlineNew(onlineNew);
		var bb = ByteBuffer.Allocate(256);
		load.encode(bb);

		var loadServer = new BServerLoad();
		loadServer.ip = this.online.providerApp.directIp;
		loadServer.port = this.online.providerApp.directPort;
		loadServer.param = new Binary(bb);

		this.online.providerApp.zeze.getServiceManagerAgent().setServerLoad(loadServer);
	}
}
