
using System;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Services.ServiceManager;
using System.Collections.Generic;

namespace Zeze.Arch
{
    public abstract class ProviderImplement : AbstractProviderImplement
    {
        public ProviderApp ProviderApp { get; set; }

        internal void ApplyOnChanged(Agent.SubscribeState subState)
        {
            if (subState.ServiceName.Equals(ProviderApp.LinkdServiceName))
            {
                ProviderApp.ProviderService.ApplyLinksChanged(subState.ServiceInfos);
            }
            /*
            else if (subState.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)){
                System.out.println("ServerId=" + ProviderApp.Zeze.getConfig().getServerId()
                + " OnChanged=" + subState.getServiceInfos());
                //this.ProviderApp.ProviderDirectService.TryConnectAndSetReady(subState, subState.getServiceInfos());
            }
            */
        }

        internal void ApplyOnPrepare(Agent.SubscribeState subState)
        {
            var pending = subState.ServiceInfosPending;
            if (pending == null)
                return;

            if (pending.ServiceName.StartsWith(ProviderApp.ServerServiceNamePrefix))
            {
                this.ProviderApp.ProviderDirectService.TryConnectAndSetReady(subState, pending);
            }
        }

        /**
         * 注册所有支持的模块服务。
         * 包括静态动态。
         * 注册的模块时带上用于Provider之间连接的ip，port。
         * <p>
         * 订阅Linkd服务。
         * Provider主动连接Linkd。
         */
        public async Task RegisterModulesAndSubscribeLinkd()
        {
            var sm = ProviderApp.Zeze.ServiceManagerAgent;
            var services = new Dictionary<string, BModule>();

            // 注册本provider的静态服务
            foreach (var it in ProviderApp.StaticBinds)
            {
                var name = $"{ProviderApp.ServerServiceNamePrefix}{it.Key}";
                var identity = ProviderApp.Zeze.Config.ServerId.ToString();
                await sm.RegisterService(name, identity, ProviderApp.DirectIp, ProviderApp.DirectPort);
                services.Add(name, it.Value);
            }
            // 注册本provider的动态服务
            foreach (var it in ProviderApp.DynamicModules)
            {
                var name = $"{ProviderApp.ServerServiceNamePrefix}{it.Key}";
                var identity = ProviderApp.Zeze.Config.ServerId.ToString();
                await sm.RegisterService(name, identity, ProviderApp.DirectIp, ProviderApp.DirectPort);
                services.Add(name, it.Value);
            }

            // 订阅provider直连发现服务
            foreach (var e in services)
            {
                await sm.SubscribeService(e.Key, e.Value.SubscribeType);
            }

            // 订阅linkd发现服务。
            await sm.SubscribeService(ProviderApp.LinkdServiceName, SubscribeInfo.SubscribeTypeSimple);
        }


        public static void SendKick(AsyncSocket sender, long linkSid, int code, string desc)
        {
            var p = new Kick();
            p.Argument.Linksid = linkSid;
            p.Argument.Code = code;
            p.Argument.Desc = desc;
            p.Send(sender);
        }

        protected override async Task<long> ProcessDispatch(Protocol _p)
        {
            var p = _p as Dispatch;
            try
            {
                var factoryHandle = ProviderApp.ProviderService.FindProtocolFactoryHandle(p.Argument.ProtocolType);
                if (null == factoryHandle)
                {
                    SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolUnkown, "unknown protocol");
                    return Procedure.LogicError;
                }
                var p2 = factoryHandle.Factory();
                p2.Service = p.Service;
                p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.ProtocolData));
                p2.Sender = p.Sender;

                var session = new ProviderUserSession(
                    ProviderApp.ProviderService,
                    p.Argument.Account,
                    p.Argument.States,
                    p.Sender,
                    p.Argument.LinkSid);

                p2.UserState = session;
                if (Transaction.Transaction.Current != null)
                {
                    // 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
                    Transaction.Transaction.Current.TopProcedure.ActionName = p2.GetType().FullName;
                    Transaction.Transaction.Current.TopProcedure.UserState = p2.UserState;
                    return await Zeze.Util.Mission.CallAsync(
                        factoryHandle.Handle,
                        p2,
                        (p, code) => { p.ResultCode = code; session.SendResponse(p); });
                }

                if (p2.Sender.Service.Zeze == null || factoryHandle.NoProcedure)
                {
                    // 应用框架不支持事务或者协议配置了“不需要事务”
                    return await Zeze.Util.Mission.CallAsync(
                        factoryHandle.Handle,
                        p2,
                        (p, code) => { p.ResultCode = code; session.SendResponse(p); });
                }

                // 创建存储过程并且在当前线程中调用。
                return await Zeze.Util.Mission.CallAsync(
                    p2.Sender.Service.Zeze.NewProcedure(
                        () => factoryHandle.Handle(p2),
                        p2.GetType().FullName,
                        factoryHandle.TransactionLevel,
                        p2.UserState),
                    p2,
                    (p, code) => { p.ResultCode = code; session.SendResponse(p); }
                    );
            }
            catch (Exception ex)
            {
                SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolException, ex.ToString());
                throw;
            }
        }

        protected override async Task<long> ProcessAnnounceLinkInfo(Zeze.Net.Protocol _p)
        {
            // reserve
            /*
            var protocol = _p as AnnounceLinkInfo;
            var linkSession = protocol.Sender.UserState as ProviderService.LinkSession;
            */
            return Procedure.Success;
        }
    }
}
