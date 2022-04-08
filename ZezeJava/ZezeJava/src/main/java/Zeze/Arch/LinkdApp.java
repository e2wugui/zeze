package Zeze.Arch;

import Zeze.Beans.Provider.BLoad;
import Zeze.Serialize.ByteBuffer;

public class LinkdApp {
	public String LinkdServiceName;
	public Zeze.Application Zeze;
	public LinkdProvider LinkdProvider;
	public LinkdProviderService LinkdProviderService;
	public LinkdService LinkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public String ProviderIp;
	public int ProviderPort;

	public LinkdApp(String linkdServiceName,
					Zeze.Application zeze, LinkdProvider linkdProvider,
					LinkdProviderService linkdProviderService, LinkdService linkdService,
					LoadConfig LoadConfig) {
		LinkdServiceName = linkdServiceName;
		Zeze = zeze;
		LinkdProvider = linkdProvider;
		LinkdProvider.LinkdApp = this;
		LinkdProviderService = linkdProviderService;
		LinkdProviderService.LinkdApp = this;
		LinkdService = linkdService;
		LinkdService.LinkdApp = this;

		LinkdProvider.Distribute = new ProviderDistribute();
		LinkdProvider.Distribute.ProviderService = LinkdProviderService;
		LinkdProvider.Distribute.Zeze = Zeze;
		LinkdProvider.Distribute.LoadConfig = LoadConfig;

		LinkdProvider.RegisterProtocols(LinkdProviderService);

		this.Zeze.getServiceManagerAgent().setOnSetServerLoad((serverLoad) -> {
			var ps = this.LinkdProviderService.ProviderSessions.get(serverLoad.getName());
			if (null != ps) {
				var bb = ByteBuffer.Wrap(serverLoad.Param);
				var load = new BLoad();
				load.Decode(bb);
				ps.Load = load;
			}
		});

		var kv = LinkdProviderService.GetOnePassiveAddress();
		ProviderIp = kv.getKey();
		ProviderPort = kv.getValue();
	}

	public String GetName() {
		return LinkdServiceName + "." + ProviderIp + ":" + ProviderPort;
	}

	public void RegisterService(Zeze.Net.Binary extra) throws Throwable {
		var identity = ProviderIp + ":" + ProviderPort;
		Zeze.getServiceManagerAgent().RegisterService(LinkdServiceName, identity,
				ProviderIp, ProviderPort, extra);
	}
}
