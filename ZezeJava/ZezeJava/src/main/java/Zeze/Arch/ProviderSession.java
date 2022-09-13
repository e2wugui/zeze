package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BLoad;

public class ProviderSession {
	volatile BLoad Load = new BLoad();
	int ServerId;
	long SessionId;
	String ServerLoadIp = "";
	int ServerLoadPort;

	/**
	 * 下面维护和本Session相关的订阅Ready状态。在Session关闭时需要取消Ready状态。
	 * 【仅用于ProviderApp】
	 */
	final ConcurrentHashMap<String, ConcurrentHashMap<String, ProviderModuleState>> ServiceReadyStates = new ConcurrentHashMap<>();

	public String getServerLoadName() {
		return ServerLoadIp + ":" + ServerLoadPort;
	}

	@Override
	public String toString() {
		return ServerLoadIp + ":" + ServerLoadPort + "@" + SessionId;
	}

	public final long getSessionId() {
		return SessionId;
	}

	public final int getServerId() {
		return ServerId;
	}

	public ConcurrentHashMap<String, ProviderModuleState> GetOrAddServiceReadyState(String serviceName) {
		return ServiceReadyStates.computeIfAbsent(serviceName, __ -> new ConcurrentHashMap<>());
	}
}
