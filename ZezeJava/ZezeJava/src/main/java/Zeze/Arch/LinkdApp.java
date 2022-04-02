package Zeze.Arch;

public class LinkdApp {
	public Zeze.Application Zeze;
	public ProviderLinkd ProviderLinkd;
	public LinkdProviderService LinkdProviderService;
	public LinkdService LinkdService;

	public LinkdApp(Zeze.Application zeze, ProviderLinkd providerLinkd,
					LinkdProviderService linkdProviderService, LinkdService linkdService,
					LoadConfig LoadConfig) {
		Zeze = zeze;
		ProviderLinkd = providerLinkd;
		ProviderLinkd.LinkdApp = this;
		LinkdProviderService = linkdProviderService;
		LinkdProviderService.LinkdApp =this;
		LinkdService = linkdService;
		LinkdService.LinkdApp = this;

		ProviderLinkd.Distribute = new ProviderDistribute();
		ProviderLinkd.Distribute.ProviderService = LinkdProviderService;
		ProviderLinkd.Distribute.Zeze = Zeze;
		ProviderLinkd.Distribute.LoadConfig = LoadConfig;

		ProviderLinkd.RegisterProtocols(LinkdProviderService);
	}

	public void RegisterService(String name, String identity, String ip, int port, String extra) throws Throwable {
		Zeze.getServiceManagerAgent().RegisterService(name, identity, ip, port, extra);
	}
}
