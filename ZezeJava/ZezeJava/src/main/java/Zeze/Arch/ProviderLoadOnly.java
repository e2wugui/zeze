package Zeze.Arch;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

import Zeze.Application;

/**
 * 无在线表的 Provider 负载上报实现：在线数与登录次数恒为 0。
 */
public class ProviderLoadOnly extends LoadBase {

	public ProviderLoadOnly(Application zeze) {
		super(zeze);
	}

	@Override
	public int getOnlineLocalCount() {
		return 0;
	}

	@Override
	public long getOnlineLoginTimes() {
		return 0;
	}

	@Override
	public LoadConfig getLoadConfig() {
		return Objects.requireNonNull(Objects.requireNonNull(getZeze().getProviderApp()).distribute).loadConfig;
	}

	@Override
	public @Nullable String getServiceIp() {
		return Objects.requireNonNull(getZeze().getProviderApp()).directIp;
	}

	@Override
	public int getServicePort() {
		return Objects.requireNonNull(getZeze().getProviderApp()).directPort;
	}
}
