package Zeze.Arch;

import java.util.Objects;

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
import org.jspecify.annotations.Nullable;
import Zeze.Builtin.LinksInfo.BLinkInfo.Data;

/**
 * Linkd 应用组装入口：聚合 LinkdService、LinkdProvider 与负载上报，并向服务管理器注册链接服务。
 */
public class LinkdApp {
	public final String linkdServiceName;
	public final Application zeze;
	public final LinkdProvider linkdProvider;
	public final LinkdProviderService linkdProviderService;
	public final LinkdService linkdService;
	// 这两个参数可以自动设置，但不够可靠，生产环境最好手动设置。
	public final String providerIp;
	public int providerPort;
	public @Nullable Action1<ServerSocket> onServerSocketBindAction;
	private @Nullable LinkdLoad linkdLoad;

	public interface DiscardAction {
		boolean call(AsyncSocket sender, int moduleId, int protocolId, int size, double rate);
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
	public final CommandConsoleService commandConsoleService;

	public LinkdApp(String linkdServiceName,
					Application zeze,
					LinkdProvider linkdProvider,
					LinkdProviderService linkdProviderService,
					LinkdService linkdService,
					LoadConfig loadConfig) {
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

		Objects.requireNonNull(this.zeze.getServiceManager()).setOnChanged(this::applyOnChanged);

		Objects.requireNonNull(this.zeze.getServiceManager()).setOnSetServerLoad(serverLoad -> {
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

		var agentConf = zeze.getConfig().getServiceConf("LoginQueueAgent");
		if (null != agentConf) {
			linkdLoad = new LinkdLoad(this);
		}
	}

	void applyOnChanged(BEditService edit) {
		for (var r : edit.getRemove()) {
			Objects.requireNonNull(linkdProvider.distributes).removeServer(r);
		}
		for (var p : edit.getAdd()) {
			Objects.requireNonNull(linkdProvider.distributes).addServer(p);
		}
	}

	public String getName() {
		return linkdServiceName + "." + providerIp + "_" + providerPort;
	}

	public void registerService(@Nullable Data extra) throws Exception {
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
		Objects.requireNonNull(zeze.getServiceManager()).editService(edit);

		// 启动load服务和LoginQueueAgent网络服务。
		// LinkdApp没有stop，不停止这两个服务。
		if (null != linkdLoad) {
			//noinspection DataFlowIssue
			linkdLoad.getLoginQueueAgent().start();
			linkdLoad.start();
		}
	}

	public @Nullable LinkdLoad getLinkdLoad() {
		return linkdLoad;
	}
}
