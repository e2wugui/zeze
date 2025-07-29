package Zeze.Arch;

import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.LoginQueueAgent;
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
	private LoginQueueAgent loginQueueAgent;

	public Application getZeze() {
		return zeze;
	}

	public ProviderLoadBase(Application zeze) {
		this.zeze = zeze;
	}

	public void setLoginQueueAgent(LoginQueueAgent loginQueueAgent) {
		this.loginQueueAgent = loginQueueAgent;
	}

	public LoginQueueAgent getLoginQueueAgent() {
		return loginQueueAgent;
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

	public final void stop() throws Exception {
		if (timerTask != null) {
			timerTask.cancel(true);
			timerTask = null;
		}
		overload.close();
		if (null != loginQueueAgent)
			loginQueueAgent.stop();
	}

	public abstract int getOnlineLocalCount();

	public abstract long getOnlineLoginTimes();

	public abstract LoadConfig getLoadConfig();

	public abstract String getServiceIp();

	public abstract int getServicePort();

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
			report(overload, online, onlineNewPerSecond);
			start(2);
			return;
		}
		if (onlineNewPerSecond > config.getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			report(overload, online, onlineNewPerSecond);
			// new delay for digestion
			start(onlineNewPerSecond / config.getMaxOnlineNew() + config.getDigestionDelayExSeconds());
			// 消化完后，下一次强迫报告Load。
			reportDelaySeconds = config.getReportDelaySeconds();
			return;
		}
		if (online > config.getProposeMaxOnline()) {
			// 在线数量超过建议最大在线，马上报告。
			report(overload, online, onlineNewPerSecond);
			start(2);
			// 超过最大建议值，强迫报告。
			reportDelaySeconds = config.getReportDelaySeconds();
			return;
		}
		// slow report
		reportDelaySeconds += timeoutDelaySeconds;
		if (reportDelaySeconds >= config.getReportDelaySeconds()) {
			reportDelaySeconds = 0;
			report(overload, online, onlineNewPerSecond);
		}
		start();
	}

	public void report(int overload, int online, int onlineNew) {
		var load = new BLoad.Data();

		load.setOverload(overload);
		load.setOnline(online);
		load.setProposeMaxOnline(getLoadConfig().getProposeMaxOnline());
		load.setOnlineNew(onlineNew);
		load.setMaxOnlineNew(getLoadConfig().getMaxOnlineNew());
		var bb = ByteBuffer.Allocate(256);
		load.encode(bb);

		// 下面两个报告原则上可以只报告一个。
		// 当启用LoginQueue，原来的load报告可以去掉了。
		// 为了兼容，先保留。

		// 向ServiceManager报告load
		var loadServer = new BServerLoad();
		loadServer.ip = getServiceIp();
		loadServer.port = getServicePort();
		loadServer.param = new Binary(bb);

		//noinspection DataFlowIssue
		this.zeze.getServiceManager().setServerLoad(loadServer);

		// 向LoginQueueServer报告load。
		if (loginQueueAgent != null)
			loginQueueAgent.reportProviderLoad(load);
	}
}
