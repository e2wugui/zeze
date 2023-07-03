using System;
using System.Collections.Concurrent;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Arch
{
	public class LinkdProviderService : Zeze.Services.HandshakeServer
	{
		// private static readonly ILogger logger = LogManager.GetLogger(typeof(LinkdProviderService));
		public LinkdApp LinkdApp { get; set; }
		public ConcurrentDictionary<string, ProviderSession> ProviderSessions = new();

		public LinkdProviderService(string name, Application zeze)
			: base(name, zeze)
		{

		}

		// 重载需要的方法。
		public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
		{
			_ = Mission.CallAsync(factoryHandle.Handle, p);
		}

		public override void OnSocketAccept(AsyncSocket sender)
        {
			sender.UserState = new LinkdProviderSession(sender.SessionId);
			base.OnSocketAccept(sender);
        }

        public override void OnHandshakeDone(AsyncSocket sender)
		{
			base.OnHandshakeDone(sender);

			var announce = new AnnounceLinkInfo();
			sender.Send(announce);
		}

		public override void OnSocketClose(AsyncSocket so, Exception e)
		{
			// 先unbind。这样避免有时间窗口。
			LinkdApp.LinkdProvider.OnProviderClose(so);
			base.OnSocketClose(so, e);
		}
	}
}
