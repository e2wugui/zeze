package Game;

import java.util.concurrent.atomic.AtomicLong;

/**
 定时向所有的 linkd 报告负载。
 如果启用cahce-sync，可能linkd数量比较多。所以正常情况下，报告间隔应长点。比如10秒。
*/
public class Load {
	private AtomicLong LoginCount = new AtomicLong();
	public final AtomicLong getLoginCount() {
		return LoginCount;
	}
	private AtomicLong LogoutCount = new AtomicLong();
	public final AtomicLong getLogoutCount() {
		return LogoutCount;
	}

	private long LoginCountLast;
	private int ReportDelaySeconds;
	private int TimoutDelaySeconds;


	public final void StartTimerTask() {
		StartTimerTask(1);
	}

	public final void StartTimerTask(int delaySeconds) {
		TimoutDelaySeconds = delaySeconds;
		Zeze.Util.Task.schedule((thisTask) -> OnTimerTask(thisTask), TimoutDelaySeconds * 1000, -1);
	}

	private void OnTimerTask(Zeze.Util.Task ThisTask) {
		long login = getLoginCount().get();
		long logout = getLogoutCount().get();
		int online = (int)(login - logout);
		int onlineNew = (int)(login - LoginCountLast);
		LoginCountLast = login;

		int onlineNewPerSecond = onlineNew / TimoutDelaySeconds;
		if (onlineNewPerSecond > App.Instance.getConfig().getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			App.getInstance().getServer().ReportLoad(online, App.getInstance().getConfig().getProposeMaxOnline(), onlineNew);
			// new delay for digestion
			StartTimerTask(onlineNewPerSecond / App.getInstance().getConfig().getMaxOnlineNew() + App.getInstance().getConfig().getDigestionDelayExSeconds());
			// 消化完后，下一次强迫报告Load。
			ReportDelaySeconds = App.getInstance().getConfig().getReportDelaySeconds();
			return;
		}
		// slow report
		ReportDelaySeconds += TimoutDelaySeconds;
		if (ReportDelaySeconds >= App.getInstance().getConfig().getReportDelaySeconds()) {
			ReportDelaySeconds = 0;
			App.getInstance().getServer().ReportLoad(online, App.getInstance().getConfig().getProposeMaxOnline(), onlineNew);
		}
		StartTimerTask();
	}
}