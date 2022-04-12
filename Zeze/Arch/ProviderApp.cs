
using System.Collections.Generic;
using Zeze.Beans.Provider;

namespace Zeze.Arch
{
	/**
	 * 记录实现一个Provider需要的对象，
	 * 设置相关对象之间的引用，
	 * 初始化。
	 */
	public class ProviderApp {
		public Zeze.Application Zeze;

		public ProviderImplement ProviderImplement;
		public ProviderService ProviderService;
		public string ServerServiceNamePrefix;

		public ProviderDirect ProviderDirect;
		public ProviderDirectService ProviderDirectService;

		public string LinkdServiceName;

		// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
		public string DirectIp;
		public int DirectPort;

		public ProviderDistribute Distribute;

		public Dictionary<int, BModule> StaticBinds = new();
		public Dictionary<int, BModule> DynamicModules = new();
		public Dictionary<int, BModule> Modules = new();

		public ProviderApp(Zeze.Application zeze,
						   ProviderImplement server,
						   ProviderService toLinkdService,
						   string providerModulePrefixNameOnServiceManager,
						   ProviderDirect direct,
						   ProviderDirectService toOtherProviderService,
						   string linkdNameOnServiceManager,
						   LoadConfig loadConfig
						   ) {
			this.Zeze = zeze;
			this.Zeze.Redirect = new RedirectBase(this);

			this.ProviderImplement = server;
			this.ProviderImplement.ProviderApp = this;
			this.ProviderService = toLinkdService;
			this.ProviderService.ProviderApp = this;
			this.ServerServiceNamePrefix = providerModulePrefixNameOnServiceManager;

			this.ProviderDirect = direct;
			this.ProviderDirect.ProviderApp = this;
			this.ProviderDirectService = toOtherProviderService;
			this.ProviderDirectService.ProviderApp = this;

			var kv = ProviderDirectService.GetOnePassiveAddress();
			this.DirectIp = kv.getKey();
			this.DirectPort = kv.getValue();

			this.LinkdServiceName = linkdNameOnServiceManager;

			this.ProviderImplement.RegisterProtocols(ProviderService);

			this.Zeze.ServiceManagerAgent.OnChanged = ProviderImplement.ApplyOnChanged;
			this.Zeze.ServiceManagerAgent.OnPrepare = ProviderImplement.ApplyOnPrepare;

			this.Zeze.ServiceManagerAgent.OnSetServerLoad = (serverLoad)-> {
				var ps = ProviderDirectService.ProviderSessions.get(serverLoad.Name);
				if (ps != null) {
					var load = new BLoad();
					load.Decode(serverLoad.Param.Wrap());
					ps.Load = load;
				}
			});
			this.Distribute = new ProviderDistribute();
			this.Distribute.LoadConfig = loadConfig;
			this.Distribute.Zeze = Zeze;
			this.Distribute.ProviderService = ProviderService;

			this.ProviderDirect.RegisterProtocols(ProviderDirectService);
		}

		public void initialize(ProviderModuleBinds binds, Dictionary<string, Zeze.IModule> modules) {
			binds.BuildStaticBinds(modules, Zeze.Config.ServerId, StaticBinds);
			binds.BuildDynamicBinds(modules, Zeze.Config.ServerId, DynamicModules);
			foreach (var e in StaticBinds)
				Modules.Add(e.Key, e.Value);
			foreach (var e in DynamicModules)
				Modules.Add(e.Key, e.Value);
		}

		public void StartLast() {
			ProviderImplement.RegisterModulesAndSubscribeLinkd();
		}
	}
}