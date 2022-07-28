package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Serialize.ByteBuffer;
import Zeze.Web.HttpAuth;

public class LinkdApp {
	public final String LinkdServiceName;
	public final Zeze.Application Zeze;
	public final LinkdProvider LinkdProvider;
	public final LinkdProviderService LinkdProviderService;
	public final LinkdService LinkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public final String ProviderIp;
	public final int ProviderPort;
	public final ConcurrentHashMap<String, HttpAuth> WebAuth = new ConcurrentHashMap<>();

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

		Zeze.getServiceManagerAgent().setOnChanged(LinkdProvider.Distribute::ApplyServers);
		Zeze.getServiceManagerAgent().setOnUpdate(LinkdProvider.Distribute::AddServer);
		Zeze.getServiceManagerAgent().setOnRemoved(LinkdProvider.Distribute::RemoveServer);

		Zeze.getServiceManagerAgent().setOnSetServerLoad((serverLoad) -> {
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

	public void RegisterService(Zeze.Net.Binary extra) {
		var identity = "@" + ProviderIp + ":" + ProviderPort;
		Zeze.getServiceManagerAgent().RegisterService(LinkdServiceName, identity,
				ProviderIp, ProviderPort, extra);
	}
}
