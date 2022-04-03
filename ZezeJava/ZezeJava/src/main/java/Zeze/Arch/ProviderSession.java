package Zeze.Arch;

import Zeze.Beans.Provider.BAnnounceProviderInfo;
import Zeze.Beans.Provider.BLoad;

public class ProviderSession {
	private BAnnounceProviderInfo Info;
	public final BAnnounceProviderInfo getInfo() {
		return Info;
	}

	public final void setInfo(BAnnounceProviderInfo value) {
		Info = value;
	}

	public volatile BLoad Load;

	private long SessionId;
	public final long getSessionId() {
		return SessionId;
	}

	public ProviderSession(long ssid) {
		SessionId = ssid;
	}
}
