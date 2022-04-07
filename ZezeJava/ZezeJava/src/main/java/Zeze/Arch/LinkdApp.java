package Zeze.Arch;

public class LinkdApp {
	public String LinkdServiceName;
	public Zeze.Application Zeze;
	public LinkdProvider LinkdProvider;
	public LinkdProviderService LinkdProviderService;
	public LinkdService LinkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public String ProviderPassiveIp;
	public int ProviderPassivePort;

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
