package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.IModule;
import Zeze.Services.ServiceManager.Load;
import org.apache.commons.lang3.NotImplementedException;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
 */
public abstract class ModuleRedirectBase {
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

	public ModuleRedirectBase(ProviderApp app) {
		ProviderApp = app;
	}
}
