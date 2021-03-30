

using System.Collections.Concurrent;
using System.Collections.Generic;
using Zeze.Net;

namespace Game
{
    public sealed partial class Server
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private Zeze.Config.ServiceConf.Connector FindConnector(AsyncSocket sender)
        {
            foreach (var connector in this.Config.Connectors)
            {
                if (connector.Socket == sender)
                    return connector;
            }
            return null;
        }

        /// <summary>
        /// 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
        /// 这里要求 linkName 在所有 provider 中都一样。
        /// 使用 Connector 配置得到名字，只要保证配置一样。
        /// </summary>
        /// <param name="sender"></param>
        /// <returns></returns>
        public string GetLinkName(AsyncSocket sender)
        {
            var connector = FindConnector(sender);
            return connector.HostNameOrAddress + " " + connector.Port;
        }

        public ConcurrentDictionary<string, AsyncSocket> Links { get; } = new ConcurrentDictionary<string, AsyncSocket>();

        // 用来同步等待Provider的静态绑定完成。
        public System.Threading.ManualResetEvent ProviderStaticBindCompleted = new System.Threading.ManualResetEvent(false);

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            base.OnHandshakeDone(sender);
            var linkName = GetLinkName(sender);
            Links[linkName] = sender;
            sender.UserState = linkName;

            // static binds
            var rpc = new gnet.Provider.Bind();
            rpc.Argument.Modules.AddRange(Game.App.Instance.StaticBinds);
            rpc.Send(sender, (protocol) => { ProviderStaticBindCompleted.Set(); return 0; });
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (p.TypeId == gnet.Provider.ModuleRedirect.TypeId_)
            {
                if (null != factoryHandle.Handle)
                {
                    var modureRecirect = p as gnet.Provider.ModuleRedirect;
                    if (null != Zeze && false == factoryHandle.NoProcedure)
                    {
                        Zeze.TaskOneByOneByKey.Execute(modureRecirect.Argument.HashCode,
                            Zeze.NewProcedure(() => factoryHandle.Handle(p), p.GetType().FullName, p.UserState));
                    }
                    else
                    {
                        Zeze.TaskOneByOneByKey.Execute(modureRecirect.Argument.HashCode,
                            () => factoryHandle.Handle(p), p.GetType().FullName);
                    }
                }
                else
                {
                    logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
                }
                return;
            }

            base.DispatchProtocol(p, factoryHandle);
        }

        public override void OnSocketClose(AsyncSocket so, System.Exception e)
        {
            base.OnSocketClose(so, e);
            Links.TryRemove(KeyValuePair.Create(GetLinkName(so), so)); // .NET 5.0
        }
    }
}
