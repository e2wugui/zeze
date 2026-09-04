package Zeze.Arch;

import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.LoginQueueAgent;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Util.TaskSpec;
import org.jetbrains.annotations.NotNull;

public abstract class LoadBase {
	private long lastLoginTime;
	private int reportDelaySeconds;
	private int timeoutDelaySeconds;
	private Future<?> timerTask;
	// 停机标志。定时器是onTimerTask自续的链条：stop置位后，已排定的下一次触发进门即返回、不再重排，
	// 链最多多醒一次即自然终止，stop无需与重排竞争取消。volatile保证定时线程醒来即见。
	private volatile boolean stopped;
	private final Application zeze;
	private final ProviderOverload overload = new ProviderOverload();
	// volatile：setup线程写一次（如ProviderApp.startLast中的setLoginQueueAgent），定时线程(report)、
	// 停机线程(stop)、choiceProvider等多线程读，无锁发布。
	private volatile LoginQueueAgent loginQueueAgent;

	public Application getZeze() {
		return zeze;
	}

	public LoadBase(Application zeze) {
		this.zeze = zeze;
	}

	public void setLoginQueueAgent(LoginQueueAgent loginQueueAgent) {
		this.loginQueueAgent = loginQueueAgent;
		if (null != loginQueueAgent)
			loginQueueAgent.setOnConnected(this::reportNow);
	}

	public LoginQueueAgent getLoginQueueAgent() {
		return loginQueueAgent;
	}

	public @NotNull ProviderOverload getOverload() {
		return overload;
	}

	public final void start() {
		start(getLoadConfig().getDigestionDelayExSeconds());
	}

	public final synchronized void start(int delaySeconds) {
		stopped = false; // 先复位标志再重排，支持stop后重启。仅外部显式重启允许复位stopped。
		resume(delaySeconds);
	}

	// FND2-A1-6：onTimerTask进门检查通过后、尾部重排前，stop()可能并发置位stopped；链内重排若走
	// start()会把stopped复位为false，链条复活且此后永不停（停机后定时链泄漏）。链内改走本方法：
	// 不复位stopped，重排的下次触发进门即返回，链自然终止。start()的复位语义保留给外部stop后重启。
	private synchronized void resume(int delaySeconds) {
		timeoutDelaySeconds = delaySeconds;
		if (null != timerTask)
			timerTask.cancel(false);
		timerTask = TaskSpec.ofAction(this::onTimerTask).scheduleNow(timeoutDelaySeconds * 1000L);
	}

	public final void stop() throws Exception {
		// 零锁：不与onTimerTask的重排竞争，靠stopped让链条自灭。停机后最多多醒一次（进门即返回）。
		stopped = true;
		overload.close();
		if (null != loginQueueAgent)
			loginQueueAgent.stop();
	}

	public abstract int getOnlineLocalCount();

	public abstract long getOnlineLoginTimes();

	public abstract LoadConfig getLoadConfig();

	public abstract String getServiceIp();

	public abstract int getServicePort();

	/** 立即上报一次负载。agent连上LoginQueue时回调（否则首次上报要等reportDelaySeconds的定期上报，默认2秒+分配tick）；也用于reconnect后重新宣告。 */
	public final synchronized void reportNow() {
		int online = getOnlineLocalCount();
		long loginTimes = getOnlineLoginTimes();
		int onlineNewPerSecond = (int)((loginTimes - lastLoginTime) / Math.max(1, timeoutDelaySeconds));
		lastLoginTime = loginTimes;
		report(overload.getOverload(), online, onlineNewPerSecond);
	}

	private synchronized void onTimerTask() {
		if (stopped)
			return; // 链在此断开：不再重排。
		var overload = this.overload.getOverload();
		int online = getOnlineLocalCount();
		long loginTimes = getOnlineLoginTimes();
		int onlineNew = (int)(loginTimes - lastLoginTime);
		lastLoginTime = loginTimes;
		int onlineNewPerSecond = onlineNew / Math.max(1, timeoutDelaySeconds); // 除零防护，对齐reportNow
		var config = getLoadConfig();
		if (overload != BLoad.eWorkFine) {
			// fast report
			report(overload, online, onlineNewPerSecond);
			resume(config.getDigestionDelayExSeconds());
			return;
		}
		if (onlineNewPerSecond > config.getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			report(overload, online, onlineNewPerSecond);
			// new delay for digestion
			resume(onlineNewPerSecond / config.getMaxOnlineNew() + config.getDigestionDelayExSeconds());
			// 消化完后，下一次强迫报告Load。
			reportDelaySeconds = config.getReportDelaySeconds();
			return;
		}
		if (online > config.getProposeMaxOnline()) {
			// 在线数量超过建议最大在线，马上报告。
			report(overload, online, onlineNewPerSecond);
			resume(config.getDigestionDelayExSeconds());
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
		resume(getLoadConfig().getDigestionDelayExSeconds());
	}

	public void report(int overload, int online, int onlineNew) {
		if (stopped)
			return; // 停机窗口内不再上报（向已停的ServiceManager/LoginQueue发送只会刷错误日志）。
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
			reportLoginQueueLoad(loginQueueAgent, load);
	}

	// LinkdLoad 需要重载。
	protected void reportLoginQueueLoad(LoginQueueAgent loginQueueAgent, BLoad.Data load) {
		loginQueueAgent.reportProviderLoad(load);
	}
}
