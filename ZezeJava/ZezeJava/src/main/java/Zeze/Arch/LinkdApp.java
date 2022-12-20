package Zeze.Arch;

import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Binary;
import Zeze.Net.Selectors;
import Zeze.Serialize.ByteBuffer;

public class LinkdApp {
	public final String linkdServiceName;
	public final Zeze.Application zeze;
	public final LinkdProvider linkdProvider;
	public final LinkdProviderService linkdProviderService;
	public final LinkdService linkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public final String providerIp;
	public final int providerPort;
	public final Selectors selectors;

	public LinkdApp(String linkdServiceName, Zeze.Application zeze, LinkdProvider linkdProvider,
					LinkdProviderService linkdProviderService, LinkdService linkdService, LoadConfig loadConfig) {
		this.linkdServiceName = linkdServiceName;
		this.zeze = zeze;
		this.linkdProvider = linkdProvider;
		this.linkdProvider.linkdApp = this;
		this.linkdProviderService = linkdProviderService;
		this.linkdProviderService.linkdApp = this;
		this.linkdService = linkdService;
		this.linkdService.linkdApp = this;

		this.linkdProvider.distribute = new ProviderDistribute(zeze, loadConfig, linkdProviderService);
		this.linkdProvider.RegisterProtocols(this.linkdProviderService);

		this.zeze.getServiceManager().setOnChanged(this.linkdProvider.distribute::applyServers);
		this.zeze.getServiceManager().setOnUpdate(this.linkdProvider.distribute::addServer);
		this.zeze.getServiceManager().setOnRemoved(this.linkdProvider.distribute::removeServer);

		this.zeze.getServiceManager().setOnSetServerLoad(serverLoad -> {
			var ps = this.linkdProviderService.providerSessions.get(serverLoad.getName());
			if (ps != null) {
				var bb = ByteBuffer.Wrap(serverLoad.param);
				var load = new BLoad();
				load.decode(bb);
				ps.load = load;
			}
		});

		var kv = this.linkdProviderService.getOnePassiveAddress();
		providerIp = kv.getKey();
		providerPort = kv.getValue();

		selectors = new Selectors("LinkdApp");
		this.linkdService.setSelectors(selectors);
	}

	public String getName() {
		return linkdServiceName + "." + providerIp + ":" + providerPort;
	}

	public void registerService(Binary extra) {
		var identity = "@" + providerIp + ":" + providerPort;
		zeze.getServiceManager().registerService(linkdServiceName, identity,
				providerIp, providerPort, extra);
	}
}
