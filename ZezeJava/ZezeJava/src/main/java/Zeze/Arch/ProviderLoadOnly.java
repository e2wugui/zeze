package Zeze.Arch;

import Zeze.Application;

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
		return getZeze().getProviderApp().distribute.loadConfig;
	}

	@Override
	public String getServiceIp() {
		return getZeze().getProviderApp().directIp;
	}

	@Override
	public int getServicePort() {
		return getZeze().getProviderApp().directPort;
	}
}
