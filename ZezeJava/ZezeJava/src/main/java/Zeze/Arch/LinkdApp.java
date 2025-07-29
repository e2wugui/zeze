package Zeze.Arch;

import java.net.ServerSocket;
import Zeze.Application;
import Zeze.Builtin.LinksInfo.BLinkInfo;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Action1;
import Zeze.Util.CommandConsoleService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinkdApp {
	public final @NotNull String linkdServiceName;
	public final @NotNull Application zeze;
	public final @NotNull LinkdProvider linkdProvider;
	public final @NotNull LinkdProviderService linkdProviderService;
	public final @NotNull LinkdService linkdService;
	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public final @NotNull String providerIp;
	public int providerPort;
	public @Nullable Action1<ServerSocket> onServerSocketBindAction;
	private LinkdLoad linkdLoad;

	public interface DiscardAction {
		boolean call(@NotNull AsyncSocket sender, int moduleId, int protocolId, int size, double rate);
	}

	public @Nullable DiscardAction discardAction;

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
	public final @NotNull CommandConsoleService commandConsoleService;

	public LinkdApp(@NotNull String linkdServiceName,
					@NotNull Application zeze,
					@NotNull LinkdProvider linkdProvider,
					@NotNull LinkdProviderService linkdProviderService,
					@NotNull LinkdService linkdService,
					@NotNull LoadConfig loadConfig) {
		this.linkdServiceName = linkdServiceName;
		this.zeze = zeze;
		this.linkdProvider = linkdProvider;
		this.linkdProvider.linkdApp = this;
		this.linkdProviderService = linkdProviderService;
		this.linkdProviderService.linkdApp = this;
		this.linkdService = linkdService;
		this.linkdService.linkdApp = this;

		this.linkdProvider.distributes = new ProviderDistributeVersion(zeze, loadConfig, linkdProviderService);
		this.linkdProvider.RegisterProtocols(this.linkdProviderService);

		this.zeze.getServiceManager().setOnChanged(this::applyOnChanged);

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

		commandConsoleService = new CommandConsoleService("Zeze.Arch.CommandConsole", zeze.getConfig());

		var agentConf = zeze.getConfig().getServiceConf("LoginQueueAgentService");
		if (null != agentConf) {
			linkdLoad = new LinkdLoad(this);
		}
	}

	void applyOnChanged(@NotNull BEditService edit) {
		for (var r : edit.getRemove()) {
			linkdProvider.distributes.removeServer(r);
		}
		for (var p : edit.getAdd()) {
			linkdProvider.distributes.addServer(p);
		}
	}

	public @NotNull String getName() {
		return linkdServiceName + "." + providerIp + "_" + providerPort;
	}

	public void registerService(@Nullable BLinkInfo.Data extra) throws Exception {
		commandConsoleService.start();
		var linkInfo = extra;
		if (null == linkInfo)
			linkInfo = new BLinkInfo.Data();

		if (linkInfo.getIp().isBlank()) {
			var passive = linkdService.getOnePassiveAddress();
			linkInfo.setIp(passive.getKey());
			linkInfo.setPort(passive.getValue());
		}
		var bb = ByteBuffer.Allocate();
		linkInfo.encode(bb);

		var identity = "@" + providerIp + "_" + providerPort;
		var edit = new BEditService();
		// linkService 总是使用版本0，不开启AppVersion.
		edit.getAdd().add(new BServiceInfo(linkdServiceName, identity, 0, providerIp, providerPort, new Binary(bb)));
		zeze.getServiceManager().editService(edit);

		// 启动load服务和LoginQueueAgent网络服务。
		// LinkdApp没有stop，不停止这两个服务。
		if (null != linkdLoad) {
			linkdLoad.getLoginQueueAgent().start();
			linkdLoad.start();
		}
	}

	public LinkdLoad getLinkdLoad() {
		return linkdLoad;
	}
}
