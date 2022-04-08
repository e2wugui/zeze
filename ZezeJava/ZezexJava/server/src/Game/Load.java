package Game;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Beans.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

/**
 定时向ServiceManager报告负载。
 其中，Provider之间互联依赖这里的Ip，Port信息。
*/
public class Load {
	private final AtomicLong LoginCount = new AtomicLong();
	public final AtomicLong getLoginCount() {
		return LoginCount;
	}
	private final AtomicLong LogoutCount = new AtomicLong();
	public final AtomicLong getLogoutCount() {
		return LogoutCount;
	}

	private long LoginCountLast;
	private int ReportDelaySeconds;
	private int TimoutDelaySeconds;
	private Future<?> TimerTask;

	public final void StartTimerTask() {
		StartTimerTask(1);
	}

	public final void StartTimerTask(int delaySeconds) {
		TimoutDelaySeconds = delaySeconds;
		if (null != TimerTask)
			TimerTask.cancel(false);
		TimerTask = Zeze.Util.Task.schedule(TimoutDelaySeconds * 1000L, this::OnTimerTask);
	}

	private void OnTimerTask() {
		long login = getLoginCount().get();
		long logout = getLogoutCount().get();
		int online = (int)(login - logout);
		int onlineNew = (int)(login - LoginCountLast);
		LoginCountLast = login;

		int onlineNewPerSecond = onlineNew / TimoutDelaySeconds;
		if (onlineNewPerSecond > App.Instance.getMyConfig().getMaxOnlineNew()) {
			// 最近上线太多，马上报告负载。linkd不会再分配用户过来。
			Report(online, onlineNew);
			// new delay for digestion
			StartTimerTask(onlineNewPerSecond / App.getInstance().getMyConfig().getMaxOnlineNew() + App.getInstance().getMyConfig().getDigestionDelayExSeconds());
			// 消化完后，下一次强迫报告Load。
			ReportDelaySeconds = App.getInstance().getMyConfig().getReportDelaySeconds();
			return;
		}
		// slow report
		ReportDelaySeconds += TimoutDelaySeconds;
		if (ReportDelaySeconds >= App.getInstance().getMyConfig().getReportDelaySeconds()) {
			ReportDelaySeconds = 0;
			Report(online, onlineNew);
		}
		StartTimerTask();
	}

	public void Report(int online, int onlineNew) {
		var load = new BLoad();
		load.setOnline(online);
		load.setProposeMaxOnline(App.getInstance().getMyConfig().getProposeMaxOnline());
		load.setOnlineNew(onlineNew);
		var bb = ByteBuffer.Allocate(256);
		load.Encode(bb);

		var loadServer = new Zeze.Services.ServiceManager.ServerLoad();
		loadServer.Ip = App.getInstance().ProviderApp.ProviderDirectPassiveIp;
		loadServer.Port = App.getInstance().ProviderApp.ProviderDirectPassivePort;
		loadServer.Param = new Binary(bb);

		try {
			App.getInstance().ProviderApp.Zeze.getServiceManagerAgent().SetServerLoad(loadServer);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
