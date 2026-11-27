package Zeze.Arch;

import java.util.Objects;

import java.util.concurrent.Future;
import org.jspecify.annotations.Nullable;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.LoginQueueAgent;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Util.TaskSpec;

/**
 * 负载上报基类：定时链周期采集在线数与过载状态，向 ServiceManager 与 LoginQueueServer 报告。
 */
public abstract class LoadBase {
	private long lastLoginTime;
	private int reportDelaySeconds;
	private int timeoutDelaySeconds;
	private @Nullable Future<?> timerTask;
	// 停机标志：置位后已排定的下次 onTimerTask 直接返回并终止自续链。
	private volatile boolean stopped;
	private final Application zeze;
	private final ProviderOverload overload = new ProviderOverload();
	private volatile @Nullable LoginQueueAgent loginQueueAgent;

	public Application getZeze() {
		return zeze;
	}

	public LoadBase(Application zeze) {
		this.zeze = zeze;
	}

	public void setLoginQueueAgent(@Nullable LoginQueueAgent loginQueueAgent) {
		this.loginQueueAgent = loginQueueAgent;
		if (null != loginQueueAgent)
			loginQueueAgent.setOnConnected(this::reportNow);
	}

	public @Nullable LoginQueueAgent getLoginQueueAgent() {
		return loginQueueAgent;
	}

	/** 获取或按需创建 LoginQueueAgent（已存在则复用，未配置 "LoginQueueAgent" 服务节时返回 null）。 */
	public final synchronized @Nullable LoginQueueAgent ensureLoginQueueAgent() {
		if (loginQueueAgent == null && zeze.getConfig().getServiceConf("LoginQueueAgent") != null)
			setLoginQueueAgent(new LoginQueueAgent(zeze.getConfig(), zeze.getConfig().getServerId(),
					Objects.requireNonNull(getServiceIp()), getServicePort()));
		return loginQueueAgent;
	}

	public ProviderOverload getOverload() {
		return overload;
	}

	public final void start() {
		start(getLoadConfig().getDigestionDelayExSeconds());
	}

	public final synchronized void start(int delaySeconds) {
		stopped = false; // 先复位标志再重排，支持 stop 后重启。
		resume(delaySeconds);
	}

	// 链内重排不复位 stopped，确保 stop() 并发置位后定时链自然终止。
	private synchronized void resume(int delaySeconds) {
		timeoutDelaySeconds = delaySeconds;
		if (null != timerTask)
			timerTask.cancel(false);
		timerTask = TaskSpec.ofAction(this::onTimerTask).scheduleNow(timeoutDelaySeconds * 1000L);
	}

	public final void stop() throws Exception {
		// 靠 stopped 标志让自续定时链自然终止，避免与重排竞争。
		stopped = true;
		overload.close();
		var agent = loginQueueAgent;
		if (agent != null)
			agent.stop();
	}

	public abstract int getOnlineLocalCount();

	public abstract long getOnlineLoginTimes();

	public abstract LoadConfig getLoadConfig();

	public abstract @Nullable String getServiceIp();

	public abstract int getServicePort();

	/** 立即上报一次负载（如 LoginQueueAgent 建连或重连后触发）。 */
	public final synchronized void reportNow() {
		int online = getOnlineLocalCount();
		long loginTimes = getOnlineLoginTimes();
		int onlineNewPerSecond = (int)((loginTimes - lastLoginTime) / Math.max(1, timeoutDelaySeconds));
		lastLoginTime = loginTimes;
		report(overload.getOverload(), online, onlineNewPerSecond);
	}

	private synchronized void onTimerTask() {
		if (stopped)
			return;
		// 统一在 finally 重排，避免异常中断自续上报链。
		var nextDelaySeconds = 0;
		try {
			var overload = this.overload.getOverload();
			int online = getOnlineLocalCount();
			long loginTimes = getOnlineLoginTimes();
			int onlineNew = (int)(loginTimes - lastLoginTime);
			lastLoginTime = loginTimes;
			int onlineNewPerSecond = onlineNew / Math.max(1, timeoutDelaySeconds);
			var config = getLoadConfig();
			if (overload != BLoad.eWorkFine) {
				// 快速上报
				report(overload, online, onlineNewPerSecond);
				nextDelaySeconds = config.getDigestionDelayExSeconds();
				return;
			}
			if (onlineNewPerSecond > config.getMaxOnlineNew()) {
				// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
				report(overload, online, onlineNewPerSecond);
				// 计算消化延迟
				nextDelaySeconds = onlineNewPerSecond / Math.max(1, config.getMaxOnlineNew())
						+ config.getDigestionDelayExSeconds();
				// 消化完后，下一次强迫报告Load。
				reportDelaySeconds = config.getReportDelaySeconds();
				return;
			}
			if (online > config.getProposeMaxOnline()) {
				// 在线数量超过建议最大在线，马上报告。
				report(overload, online, onlineNewPerSecond);
				nextDelaySeconds = config.getDigestionDelayExSeconds();
				// 超过最大建议值，强迫报告。
				reportDelaySeconds = config.getReportDelaySeconds();
				return;
			}
			// 慢速上报
			reportDelaySeconds += timeoutDelaySeconds;
			if (reportDelaySeconds >= config.getReportDelaySeconds()) {
				reportDelaySeconds = 0;
				report(overload, online, onlineNewPerSecond);
			}
		} finally {
			if (!stopped)
				resume(nextDelaySeconds != 0 ? nextDelaySeconds : getLoadConfig().getDigestionDelayExSeconds());
		}
	}

	public void report(int overload, int online, int onlineNew) {
		if (stopped)
			return;
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
		var agent = loginQueueAgent;
		if (agent != null)
			reportLoginQueueLoad(agent, load);
	}

	// LinkdLoad 需要重载。
	protected void reportLoginQueueLoad(LoginQueueAgent loginQueueAgent, BLoad.Data load) {
		loginQueueAgent.reportProviderLoad(load);
	}
}
