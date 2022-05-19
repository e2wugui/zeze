using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Arch
{
	public class LinkdProviderService : Zeze.Services.HandshakeServer
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
		public LinkdApp LinkdApp { get; set; }
		public ConcurrentDictionary<string, ProviderSession> ProviderSessions = new();

		public LinkdProviderService(string name, Application zeze)
			: base(name, zeze)
		{

		}

		// 重载需要的方法。
		public override async Task DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
		{
			if (null != factoryHandle.Handle)
			{
				if (p.TypeId == Bind.TypeId_)
				{
					// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
					// 不要直接在io-thread里面执行。
					_ = Mission.CallAsync(factoryHandle.Handle, p);
				}
				else
				{
					// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
					// 直接执行，少一次线程切换。
					try
					{
						var isRequestSaved = p.IsRequest;
						var result = await factoryHandle.Handle(p);
						Mission.LogAndStatistics(null, result, p, isRequestSaved);
					}
					catch (Exception ex)
					{
						logger.Error(ex, "Protocol.Handle Exception: " + p);
					}
				}
			}
			else
				logger.Warn("Protocol Handle Not Found: {}", p);
		}

        public override async Task OnSocketAccept(AsyncSocket sender)
        {
			sender.UserState = new LinkdProviderSession(sender.SessionId);
			await base.OnSocketAccept(sender);
        }

        public override async Task OnHandshakeDone(AsyncSocket sender)
		{
			await base.OnHandshakeDone(sender);

			var announce = new AnnounceLinkInfo();
			sender.Send(announce);
		}

		public override async Task OnSocketClose(AsyncSocket so, Exception e)
		{
			// 先unbind。这样避免有时间窗口。
			LinkdApp.LinkdProvider.OnProviderClose(so);
			await base.OnSocketClose(so, e);
		}
	}
}
