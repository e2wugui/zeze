package Zeze.Arch;

import java.net.ServerSocket;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Selectors;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action1;
import Zeze.Util.CommandConsoleService;
import Zeze.Util.Func5;

public class LinkdApp {
	public final String linkdServiceName;
	public final Zeze.Application zeze;
	public final LinkdProvider linkdProvider;
	public final LinkdProviderService linkdProviderService;
	public final LinkdService linkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public final String providerIp;
	public int providerPort;
	public final Selectors selectors;
	public Action1<ServerSocket> onServerSocketBindAction;
	public Func5<AsyncSocket, Integer, Integer, Integer, Double, Boolean> discardAction;

	/**
	 * 自动创建，自动启动。
	 * 真正要能工作，
	 * 1. 需要配置 ServiceConf，如下：
	 * <pre>
	 * <ServiceConf Name="Zeze.Arch.CommandConsole">
	 *     <Acceptor Ip="" Port="#PortNumber"/>
	 * </ServiceConf>
	 * </pre>
	 * 2. 需要调用 commandConsoleService.setCommandConsole 设置一个命令处理器进去。
	 * 这个在最好在调用 LinkdApp.registerService 之前设置，这样命令行服务就准备好。
	 * 也可以任意时候设置，但是新设置的命令处理仅在新接受的连接中生效。
	 */
	public final CommandConsoleService commandConsoleService;

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

		this.commandConsoleService = new CommandConsoleService("Zeze.Arch.CommandConsole", zeze.getConfig());
	}

	public String getName() {
		return linkdServiceName + "." + providerIp + ":" + providerPort;
	}

	public void registerService(Binary extra) throws Exception {
		this.commandConsoleService.start();

		var identity = "@" + providerIp + ":" + providerPort;
		zeze.getServiceManager().registerService(linkdServiceName, identity,
				providerIp, providerPort, extra);
	}
}
