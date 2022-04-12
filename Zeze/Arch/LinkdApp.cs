
using System.Threading.Tasks;
using Zeze.Beans.Provider;
using Zeze.Serialize;

namespace Zeze.Arch
{
	public class LinkdApp {
		public string LinkdServiceName;
		public Zeze.Application Zeze;
		public LinkdProvider LinkdProvider;
		public LinkdProviderService LinkdProviderService;
		public LinkdService LinkdService;
		// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
		public string ProviderIp;
		public int ProviderPort;

		public LinkdApp(string linkdServiceName,
						Zeze.Application zeze, LinkdProvider linkdProvider,
						LinkdProviderService linkdProviderService, LinkdService linkdService,
						LoadConfig LoadConfig) {
			LinkdServiceName = linkdServiceName;
			Zeze = zeze;
			LinkdProvider = linkdProvider;
			LinkdProvider.LinkdApp = this;
			LinkdProviderService = linkdProviderService;
			LinkdProviderService.LinkdApp = this;
			LinkdService = linkdService;
			LinkdService.LinkdApp = this;

			LinkdProvider.Distribute = new ProviderDistribute();
			LinkdProvider.Distribute.ProviderService = LinkdProviderService;
			LinkdProvider.Distribute.Zeze = Zeze;
			LinkdProvider.Distribute.LoadConfig = LoadConfig;

			LinkdProvider.RegisterProtocols(LinkdProviderService);

			this.Zeze.ServiceManagerAgent.OnSetServerLoad = (serverLoad)-> {
				var ps = this.LinkdProviderService.ProviderSessions.get(serverLoad.Name);
				if (null != ps) {
					var bb = ByteBuffer.Wrap(serverLoad.Param);
					var load = new BLoad();
					load.Decode(bb);
					ps.Load = load;
				}
			};

			var kv = LinkdProviderService.GetOnePassiveAddress();
			ProviderIp = kv.getKey();
			ProviderPort = kv.getValue();
		}

		public string GetName() {
			return LinkdServiceName + "." + ProviderIp + ":" + ProviderPort;
		}

		public async Task RegisterService(Zeze.Net.Binary extra) {
			var identity = ProviderIp + ":" + ProviderPort;
			await Zeze.ServiceManagerAgent.RegisterService(LinkdServiceName, identity, ProviderIp, ProviderPort, extra);
		}
	}
}