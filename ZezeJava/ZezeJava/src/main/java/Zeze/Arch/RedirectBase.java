package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.Gen.GenModule;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Util.Action0;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
 */
public abstract class RedirectBase {
	public ConcurrentHashMap<String, RedirectHandle> Handles = new ConcurrentHashMap <>();

	public <T extends Zeze.AppBase> IModule ReplaceModuleInstance(T userApp, IModule module) {
		return GenModule.Instance.ReplaceModuleInstance(userApp, module);
	}

	public int GetChoiceHashCode() {
		throw new UnsupportedOperationException();
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

	public AsyncSocket ChoiceHash(IModule module, int hash) {
		return null;
	}

	public void RedirectAll(IModule module, ModuleRedirectAllRequest req) {

	}

	public TaskCompletionSource<Long> RunFuture(Action0 action) {
		var future = new TaskCompletionSource<Long>();
		Task.run(() -> {
			try {
				action.run();
				future.SetResult(0L);
			} catch (Throwable ex) {
				future.SetException(ex);
			}
		}, "Redirect Loop Back Future");
		return future;
	}

	public void RunVoid(Action0 action) {
		Task.run(action, "Redirect Loop Back Async");
	}
}
