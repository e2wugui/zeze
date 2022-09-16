package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Serialize.ByteBuffer;
import Zeze.Web.HttpAuth;
import Zeze.Web.HttpService;

public class LinkdApp {
	public final String linkdServiceName;
	public final Zeze.Application zeze;
	public final LinkdProvider linkdProvider;
	public final LinkdProviderService linkdProviderService;
	public final LinkdService linkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public final String providerIp;
	public final int providerPort;
	public final ConcurrentHashMap<String, HttpAuth> webAuth = new ConcurrentHashMap<>();

	public HttpService httpService; // 可选模块，需要自己初始化。但是内部实现需要这个引用。所以定义在这里了。

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

		this.zeze.getServiceManagerAgent().setOnChanged(this.linkdProvider.distribute::applyServers);
		this.zeze.getServiceManagerAgent().setOnUpdate(this.linkdProvider.distribute::addServer);
		this.zeze.getServiceManagerAgent().setOnRemoved(this.linkdProvider.distribute::removeServer);

		this.zeze.getServiceManagerAgent().setOnSetServerLoad(serverLoad -> {
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
	}

	public String getName() {
		return linkdServiceName + "." + providerIp + ":" + providerPort;
	}

	public void registerService(Zeze.Net.Binary extra) {
		var identity = "@" + providerIp + ":" + providerPort;
		zeze.getServiceManagerAgent().registerService(linkdServiceName, identity,
				providerIp, providerPort, extra);
	}
}
