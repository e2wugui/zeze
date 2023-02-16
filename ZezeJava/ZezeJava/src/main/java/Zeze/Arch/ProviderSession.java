package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BLoad;

public class ProviderSession {
	protected volatile BLoad load = new BLoad();
	protected int serverId;
	protected long sessionId;
	protected String serverLoadIp = "";
	protected int serverLoadPort;

	/**
	 * 下面维护和本Session相关的订阅Ready状态。在Session关闭时需要取消Ready状态。
	 * 【仅用于ProviderApp】
	 */
	protected final ConcurrentHashMap<String, ConcurrentHashMap<String, ProviderModuleState>> ServiceReadyStates = new ConcurrentHashMap<>();

	public String getServerLoadName() {
		return serverLoadIp + ":" + serverLoadPort;
	}

	@Override
	public String toString() {
		return serverLoadIp + ":" + serverLoadPort + "@" + sessionId;
	}

	public long getSessionId() {
		return sessionId;
	}

	public int getServerId() {
		return serverId;
	}

	public ConcurrentHashMap<String, ProviderModuleState> getOrAddServiceReadyState(String serviceName) {
		return ServiceReadyStates.computeIfAbsent(serviceName, __ -> new ConcurrentHashMap<>());
	}
}
