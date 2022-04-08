package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
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

	/**
	 * 下面维护和本Session相关的订阅Ready状态。在Session关闭时需要取消Ready状态。
	 * 【仅用于ProviderApp】
	 */
	public ConcurrentHashMap<String, ConcurrentHashMap<String, ProviderModuleState>> ServiceReadyStates = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, ProviderModuleState> GetOrAddServiceReadyState(String serviceName) {
		return ServiceReadyStates.computeIfAbsent(serviceName, (key) -> new ConcurrentHashMap<>());
	}
}
