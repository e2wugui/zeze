package Zeze.Game;

import Zeze.Arch.LoadConfig;
import Zeze.Arch.LoadBase;

public class ProviderLoadWithOnline extends LoadBase {

	public final Online online;

	public ProviderLoadWithOnline(Online online) {
		super(online.providerApp.zeze);
		this.online = online;
	}

	@Override
	public int getOnlineLocalCount() {
		return this.online.getLocalCount();
	}

	@Override
	public long getOnlineLoginTimes() {
		return this.online.getLoginTimes();
	}

	@Override
	public LoadConfig getLoadConfig() {
		return this.online.providerApp.distribute.loadConfig;
	}

	@Override
	public String getServiceIp() {
		return this.online.providerApp.directIp;
	}

	@Override
	public int getServicePort() {
		return this.online.providerApp.directPort;
	}
}
