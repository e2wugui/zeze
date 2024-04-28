package Zeze.Arch;

import java.net.ServerSocket;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Action1;
import Zeze.Util.CommandConsoleService;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.Task;
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

		var checkPeriod = PropertiesHelper.getInt("KeepAliveCheckPeriod", 5000);
		Task.scheduleUnsafe(checkPeriod, checkPeriod, this::keepAliveCheckTimer);
	}

	void applyOnChanged(@NotNull BEditService edit) {
		for (var r : edit.getRemove()) {
			linkdProvider.distributes.removeServer(r);
		}
		for (var p : edit.getAdd()) {
			linkdProvider.distributes.addServer(p);
		}
	}

	private void keepAliveCheckTimer() throws Exception {
		var now = System.currentTimeMillis();
		var timeout = PropertiesHelper.getInt("KeepAliveTimeout", 180_000);
		linkdService.foreach((link) -> {
			var session = (LinkdUserSession)link.getUserState();
			if (null != session && session.keepAliveTimeout(now, timeout))
				link.close(Service.keepAliveException);
		});
	}

	public @NotNull String getName() {
		return linkdServiceName + "." + providerIp + "_" + providerPort;
	}

	public void registerService(@Nullable Binary extra) throws Exception {
		commandConsoleService.start();

		var identity = "@" + providerIp + "_" + providerPort;
		var edit = new BEditService();
		// linkService 总是使用版本0，不开启AppVersion.
		edit.getAdd().add(new BServiceInfo(linkdServiceName, identity, 0, providerIp, providerPort, extra));
		zeze.getServiceManager().editService(edit);
	}
}
