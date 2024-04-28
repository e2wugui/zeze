
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Serialize;
using Zeze.Services.ServiceManager;

namespace Zeze.Arch
{
	/**
	 * 记录实现一个Provider需要的对象，
	 * 设置相关对象之间的引用，
	 * 初始化。
	 */
	public class ProviderApp
	{
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

		public readonly Dictionary<int, BModule> StaticBinds = new();
		public readonly Dictionary<int, BModule> DynamicModules = new();
		public readonly Dictionary<int, BModule> Modules = new();
		public readonly Dictionary<string, IModule> BuiltinModules = new();

		public ProviderApp(Zeze.Application zeze,
						   ProviderImplement server,
						   ProviderService toLinkdService,
						   string providerModulePrefixNameOnServiceManager,
						   ProviderDirect direct,
						   ProviderDirectService toOtherProviderService,
						   string linkdNameOnServiceManager,
						   LoadConfig loadConfig
						   )
		{
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

			(DirectIp, DirectPort) = ProviderDirectService.GetOnePassiveAddress();

			this.LinkdServiceName = linkdNameOnServiceManager;

			this.ProviderImplement.RegisterProtocols(ProviderService);

			this.Zeze.ServiceManager.OnSetServerLoad = (serverLoad) =>
			{
				if (ProviderDirectService.ProviderByLoadName.TryGetValue(serverLoad.Name, out var ps))
				{
					var load = new BLoad();
					var bb = ByteBuffer.Wrap(serverLoad.Param);
					load.Decode(bb);
					ps.Load = load;
				}
			};
			var appVersion = 0; // TODO app version
			this.Distribute = new ProviderDistribute(appVersion, Zeze, ProviderDirectService, loadConfig);
			this.Zeze.ServiceManager.OnChanged = ApplyChanged;
			this.ProviderDirect.RegisterProtocols(ProviderDirectService);
		}

		private void ApplyChanged(BEditService edit)
		{
            var refresh = false;
            foreach (var r in edit.Remove)
            {
                if (r.ServiceName.Equals(LinkdServiceName))
				{
                    refresh |= ProviderService.ApplyRemove(r);
                }
                else if (r.ServiceName.StartsWith(ServerServiceNamePrefix))
                {
                    ProviderDirectService.RemoveServer(r);
                    Distribute.RemoveServer(r);
                }
            }
            foreach (var p in edit.Add)
            {
				if (p.ServiceName.Equals(LinkdServiceName))
				{
					refresh |= ProviderService.ApplyPut(p);
				}
				else if (p.ServiceName.StartsWith(ServerServiceNamePrefix))
				{
					ProviderDirectService.AddServer(p);
					Distribute.AddServer(p);
				}
            }

            if (refresh)
                ProviderService.RefreshLinkConnectors();
        }

        public string MakeServiceName(IModule module)
        {
			return ProviderDistribute.MakeServiceName(ServerServiceNamePrefix, module.Id);
        }

		public async Task StartLast(ProviderModuleBinds binds, Dictionary<string, Zeze.IModule> modules)
		{
			foreach (var builtin in BuiltinModules.Values)
				modules.Add(builtin.FullName, builtin);

			binds.BuildStaticBinds(modules, Zeze.Config.ServerId, StaticBinds);
			binds.BuildDynamicBinds(modules, Zeze.Config.ServerId, DynamicModules);
			foreach (var e in StaticBinds)
				Modules.Add(e.Key, e.Value);
			foreach (var e in DynamicModules)
				Modules.Add(e.Key, e.Value);

			await ProviderImplement.RegisterModulesAndSubscribeLinkd();
		}
	}
}