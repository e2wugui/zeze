package Zeze.Arch;

import Zeze.Beans.Provider.BAnnounceProviderInfo;
import Zeze.Beans.Provider.BLoad;

public class ProviderSession {
	private volatile BLoad Load;
	private BAnnounceProviderInfo Info;
	public final BAnnounceProviderInfo getInfo() {
		return Info;
	}
	public final void setInfo(BAnnounceProviderInfo value) {
		Info = value;
	}
	public final int getProposeMaxOnline() {
		return Load.getProposeMaxOnline();
	}
	public final int getOnline() {
		return Load.getOnline();
	}
	public final int getOnlineNew() {
		return Load.getOnlineNew();
	}

	private long SessionId;
	public final long getSessionId() {
		return SessionId;
	}

	public final void SetLoad(BLoad load) {
		Load = load.Copy(); // 复制一次吧。
	}

	public ProviderSession(long ssid) {
		SessionId = ssid;
	}
}
