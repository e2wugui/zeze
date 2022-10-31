package Zeze.Game;

import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderLoadBase;

public class ProviderLoad extends ProviderLoadBase {

	public final Online online;

	public ProviderLoad(Online online) {
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
		assert this.online.providerApp != null;
		return this.online.providerApp.distribute.loadConfig;
	}

	@Override
	public String getProviderIp() {
		assert this.online.providerApp != null;
		return this.online.providerApp.directIp;
	}

	@Override
	public int getProviderPort() {
		assert this.online.providerApp != null;
		return this.online.providerApp.directPort;
	}
}
