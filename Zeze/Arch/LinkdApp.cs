
using System;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services.ServiceManager;

namespace Zeze.Arch
{
	public class LinkdApp
	{
		public string LinkdServiceName;
		public Zeze.Application Zeze;
		public LinkdProvider LinkdProvider;
		public LinkdProviderService LinkdProviderService;
		public LinkdService LinkdService;
		// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
		public string ProviderIp;
		public int ProviderPort;
		public Func<AsyncSocket, int, int, int, double, bool> DiscardAction;

		public LinkdApp(string linkdServiceName,
						Zeze.Application zeze, LinkdProvider linkdProvider,
						LinkdProviderService linkdProviderService, LinkdService linkdService,
						LoadConfig LoadConfig)
		{
			LinkdServiceName = linkdServiceName;
			Zeze = zeze;
			LinkdProvider = linkdProvider;
			LinkdProvider.LinkdApp = this;
			LinkdProviderService = linkdProviderService;
			LinkdProviderService.LinkdApp = this;
			LinkdService = linkdService;
			LinkdService.LinkdApp = this;

            LinkdProvider.Distribute = new ProviderDistributeVersion
            {
                ProviderService = LinkdProviderService,
                Zeze = Zeze,
                LoadConfig = LoadConfig
            };

            LinkdProvider.RegisterProtocols(LinkdProviderService);

			Zeze.ServiceManager.OnChanged = ApplyChanged;
			Zeze.ServiceManager.OnSetServerLoad = (serverLoad) =>
			{
				if (this.LinkdProviderService.ProviderSessions.TryGetValue(serverLoad.Name, out var ps))
				{ 
					var bb = ByteBuffer.Wrap(serverLoad.Param);
					var load = new BLoad();
					load.Decode(bb);
					ps.Load = load;
				}
			};

			(ProviderIp, ProviderPort) = LinkdProviderService.GetOnePassiveAddress();
		}

        void ApplyChanged(BEditService edit)
		{
            foreach (var r in edit.Remove)
            {
                LinkdProvider.Distribute.RemoveServer(r);
            }
            foreach (var p in edit.Add)
            {
                LinkdProvider.Distribute.AddServer(p);
            }
            // todo process update
        }


        public string GetName()
		{
			return LinkdServiceName + "." + ProviderIp + ":" + ProviderPort;
		}

		public async Task RegisterService(Zeze.Net.Binary extra)
		{
			var identity = "@" + ProviderIp + ":" + ProviderPort;
			await Zeze.ServiceManager.RegisterService(LinkdServiceName, identity, 0, ProviderIp, ProviderPort, extra);
		}
	}
}