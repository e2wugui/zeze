package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BLoad;

public class ProviderSession {
	public volatile BLoad Load = new BLoad();
	private final long SessionId;
	public String ServerLoadIp = "";
	public int ServerLoadPort;

	public String getServerLoadName() {
		return ServerLoadIp + ":" + ServerLoadPort;
	}

	@Override
	public String toString() {
		return getServerLoadName() + "@" + SessionId;
	}

	public ProviderSession(long ssid) {
		SessionId = ssid;
	}

	public final long getSessionId() {
		return SessionId;
	}

	/**
	 * 下面维护和本Session相关的订阅Ready状态。在Session关闭时需要取消Ready状态。
	 * 【仅用于ProviderApp】
	 */
	public final ConcurrentHashMap<String, ConcurrentHashMap<String, ProviderModuleState>> ServiceReadyStates = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, ProviderModuleState> GetOrAddServiceReadyState(String serviceName) {
		return ServiceReadyStates.computeIfAbsent(serviceName, (key) -> new ConcurrentHashMap<>());
	}
}
