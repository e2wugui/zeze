package Zeze.Arch;

import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public abstract class ProviderLoadBase {
	private long lastLoginTime;
	private int reportDelaySeconds;
	private int timeoutDelaySeconds;
	private Future<?> timerTask;
	private final Application zeze;
	private final ProviderOverload overload = new ProviderOverload();

	public Application getZeze() {
		return zeze;
	}

	public ProviderLoadBase(Application zeze) {
		this.zeze = zeze;
	}

	public @NotNull ProviderOverload getOverload() {
		return overload;
	}

	public final void start() {
		start(2);
	}

	public final void start(int delaySeconds) {
		timeoutDelaySeconds = delaySeconds;
		if (null != timerTask)
			timerTask.cancel(false);
		timerTask = Task.scheduleUnsafe(timeoutDelaySeconds * 1000L, this::onTimerTask);
	}

	public final void stop() {
		if (timerTask != null) {
			timerTask.cancel(true);
			timerTask = null;
			overload.close();
		}
	}

	public abstract int getOnlineLocalCount();

	public abstract long getOnlineLoginTimes();

	public abstract LoadConfig getLoadConfig();

	public abstract String getProviderIp();

	public abstract int getProviderPort();

	private void onTimerTask() {
		var overload = this.overload.getOverload();
		int online = getOnlineLocalCount();
		long loginTimes = getOnlineLoginTimes();
		int onlineNew = (int)(loginTimes - lastLoginTime);
		lastLoginTime = loginTimes;
		int onlineNewPerSecond = onlineNew / timeoutDelaySeconds;
		var config = getLoadConfig();
		if (overload != BLoad.eWorkFine) {
			// fast report
			report(overload, online, onlineNew);
			start(2);
			return;
		}
		if (onlineNewPerSecond > config.getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			report(overload, online, onlineNew);
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
			report(overload, online, onlineNew);
		}
		start();
	}

	public void report(int overload, int online, int onlineNew) {
		var load = new BLoad();

		load.setOverload(overload);
		load.setOnline(online);
		load.setProposeMaxOnline(getLoadConfig().getProposeMaxOnline());
		load.setOnlineNew(onlineNew);
		var bb = ByteBuffer.Allocate(256);
		load.encode(bb);

		var loadServer = new BServerLoad();
		loadServer.ip = getProviderIp();
		loadServer.port = getProviderPort();
		loadServer.param = new Binary(bb);

		this.zeze.getServiceManager().setServerLoad(loadServer);
	}
}
