package Zeze.Arch;

public class ProviderLoadWithOnline extends ProviderLoadBase {
	public final Online online;

	public ProviderLoadWithOnline(Online online) {
		super(online.providerApp.zeze);
		this.online = online;
	}

	@Override
	public int getOnlineLocalCount() {
		return online.getLocalCount();
	}

	@Override
	public long getOnlineLoginTimes() {
		return online.getLoginTimes();
	}

	@Override
	public LoadConfig getLoadConfig() {
		return online.providerApp.distribute.loadConfig;
	}

	@Override
	public String getProviderIp() {
		return online.providerApp.directIp;
	}

	@Override
	public int getProviderPort() {
		return online.providerApp.directPort;
	}
}
