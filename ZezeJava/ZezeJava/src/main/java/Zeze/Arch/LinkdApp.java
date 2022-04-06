package Zeze.Arch;

public class LinkdApp {
	public String LinkdServiceName;
	public Zeze.Application Zeze;
	public ProviderLinkd ProviderLinkd;
	public LinkdProviderService LinkdProviderService;
	public LinkdService LinkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public String ProviderPassiveIp;
	public int ProviderPassivePort;

	public LinkdApp(String linkdServiceName,
					Zeze.Application zeze, ProviderLinkd providerLinkd,
					LinkdProviderService linkdProviderService, LinkdService linkdService,
					LoadConfig LoadConfig) {
		LinkdServiceName = linkdServiceName;
		Zeze = zeze;
		ProviderLinkd = providerLinkd;
		ProviderLinkd.LinkdApp = this;
		LinkdProviderService = linkdProviderService;
		LinkdProviderService.LinkdApp = this;
		LinkdService = linkdService;
		LinkdService.LinkdApp = this;

		ProviderLinkd.Distribute = new ProviderDistribute();
		ProviderLinkd.Distribute.ProviderService = LinkdProviderService;
		ProviderLinkd.Distribute.Zeze = Zeze;
		ProviderLinkd.Distribute.LoadConfig = LoadConfig;

		ProviderLinkd.RegisterProtocols(LinkdProviderService);

		var kv = LinkdProviderService.GetOnePassiveAddress();
		ProviderPassiveIp = kv.getKey();
		ProviderPassivePort = kv.getValue();
	}

	public String GetName() {
		return LinkdServiceName + "." + ProviderPassiveIp + ":" + ProviderPassivePort;
	}

	public void RegisterService(Zeze.Net.Binary extra) throws Throwable {
		var identity = ProviderPassiveIp + ":" + ProviderPassivePort;
		Zeze.getServiceManagerAgent().RegisterService(LinkdServiceName, identity,
				ProviderPassiveIp, ProviderPassivePort, extra);
	}
}
