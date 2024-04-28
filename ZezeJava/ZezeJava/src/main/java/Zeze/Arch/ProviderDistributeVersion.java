package Zeze.Arch;

import Zeze.Application;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.FewModifyMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProviderDistributeVersion {
	public final Application zeze;
	public final LoadConfig loadConfig;
	private final Service providerService;
	private final FewModifyMap<Long, ProviderDistribute> providerDistributes = new FewModifyMap<>();

	public ProviderDistributeVersion(Application zeze, LoadConfig loadConfig, Service providerService) {
		this.zeze = zeze;
		this.loadConfig = loadConfig;
		this.providerService = providerService;
	}

	public void addServer(@NotNull BServiceInfo info) {
		providerDistributes.computeIfAbsent(info.getVersion(),
				k -> new ProviderDistribute(zeze, loadConfig, providerService, k)).addServer(info);
	}

	public void removeServer(@NotNull BServiceInfo info) {
		var providerDistribute = providerDistributes.get(info.getVersion());
		if (providerDistribute != null)
			providerDistribute.removeServer(info);
	}

	public @Nullable ProviderDistribute selectDistribute(long version) {
		return providerDistributes.get(version);
	}
}
