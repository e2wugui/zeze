package Zeze.Arch;

import Zeze.Beans.Provider.BAnnounceProviderInfo;
import Zeze.Beans.Provider.BLoad;

public class ProviderSession {
	private BAnnounceProviderInfo Info;
	public volatile BLoad Load;
	private final long SessionId;

	public ProviderSession(long ssid) {
		SessionId = ssid;
	}

	public final BAnnounceProviderInfo getInfo() {
		return Info;
	}

	public final void setInfo(BAnnounceProviderInfo value) {
		Info = value;
	}

	public final long getSessionId() {
		return SessionId;
	}
}
