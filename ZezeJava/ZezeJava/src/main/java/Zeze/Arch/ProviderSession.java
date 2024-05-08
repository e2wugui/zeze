package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Util.Str;
import Zeze.Util.TimeCounter;
import org.jetbrains.annotations.NotNull;

public class ProviderSession {
	protected volatile @NotNull BLoad load = new BLoad();
	protected final TimeCounter timeCounter = new TimeCounter(5);

	protected int serverId;
	protected long sessionId;
	protected @NotNull String serverLoadIp = "";
	protected long appVersion;
	protected int serverLoadPort;
	protected boolean disableChoice = false;

	/**
	 * 下面维护和本Session相关的订阅Ready状态。在Session关闭时需要取消Ready状态。
	 * 【仅用于ProviderApp】
	 */
	protected final ConcurrentHashMap<String, ConcurrentHashMap<String, ProviderModuleState>> ServiceReadyStates = new ConcurrentHashMap<>();

	public @NotNull String getServerLoadName() {
		return serverLoadIp + '_' + serverLoadPort;
	}

	@Override
	public @NotNull String toString() {
		return '(' + serverLoadIp + ',' + serverLoadPort + ',' + sessionId + ',' + Str.toVersionStr(appVersion) + ')';
	}

	public long getAppMainVersion() {
		return appVersion >>> 48;
	}

	public long getSessionId() {
		return sessionId;
	}

	public int getServerId() {
		return serverId;
	}

	public @NotNull ConcurrentHashMap<String, ProviderModuleState> getOrAddServiceReadyState(@NotNull String serviceName) {
		return ServiceReadyStates.computeIfAbsent(serviceName, __ -> new ConcurrentHashMap<>());
	}

	public boolean isDisableChoice() {
		return disableChoice;
	}

	public void setDisableChoice(boolean value) {
		disableChoice = value;
	}
}
