package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.Gen.GenModule;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Util.OutObject;
import org.apache.commons.lang3.NotImplementedException;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
 */
public abstract class RedirectBase {
	public ConcurrentHashMap<String, RedirectHandle> Handles = new ConcurrentHashMap <>();

	public int GetDefaultChoiceType() {
		return Zeze.Beans.Provider.BModule.ChoiceTypeHashAccount;
	}

	public int GetChoiceHashCode() {
		throw new NotImplementedException("GetChoiceHashCode By Context");
	}

	public void DispatchResponse() {

	}

	public IModule ReplaceModuleInstance(IModule module) {
		return GenModule.Instance.ReplaceModuleInstance(module);
	}

	public ProviderApp ProviderApp;

	public RedirectBase(ProviderApp app) {
		ProviderApp = app;
	}

	public AsyncSocket ChoiceServer(IModule module, int serverId) {
		if (serverId == ProviderApp.Zeze.getConfig().getServerId())
			return null; // is Local
		var subs = ProviderApp.Zeze.getServiceManagerAgent().getSubscribeStates();
		var out = new OutObject<Long>();
		if (ProviderApp.Distribute.ChoiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.getId(), serverId, out))
			return ProviderApp.ProviderService.GetSocket(out.Value);
		return null;
	}
}
