
using System;

namespace gnet.Provider
{
    public sealed partial class ModuleProvider : AbstractModule
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        private void SendKick(Zeze.Net.AsyncSocket sender, long linkSid, int code, string desc)
        {
            var p = new Kick();
            p.Argument.Linksid = linkSid;
            p.Argument.Code = code;
            p.Argument.Desc = desc;
            p.Send(sender);
        }

        public override int ProcessDispatch(Dispatch p)
        {
            try
            {
                var factoryHandle = Game.App.Instance.Server.FindProtocolFactoryHandle(p.Argument.ProtocolType);
                if (null == factoryHandle)
                {
                    SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolUnkown, "unknown protocol");
                    return Zeze.Transaction.Procedure.LogicError;
                }
                var p2 = factoryHandle.Factory();
                p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.ProtocolData));
                p2.Sender = p.Sender;
                
                p2.UserState = new Game.Login.Session(p.Argument.UserId, p.Argument.States, p.Sender, p.Argument.LinkSid);

                if (Zeze.Transaction.Transaction.Current != null)
                {
                    // 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
                    Zeze.Transaction.Transaction.Current.UserState = p2.UserState;
                    return factoryHandle.Handle(p2);
                }

                if (p2.Sender.Service.Zeze == null || factoryHandle.NoProcedure)
                {
                    // 应用框架不支持事务或者协议配置了“不需要事务”
                    return factoryHandle.Handle(p2);
                }

                // 创建存储过程并且在当前线程中调用。
                p2.Sender.Service.Zeze.NewProcedure(() =>
                {
                    try
                    {
                        global::Zeze.Transaction.Transaction.Current.UserState = p2.UserState;
                        return factoryHandle.Handle(p2);
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex, "ProcessDispatch");
                        return Zeze.Transaction.Procedure.Excption;
                    }
                }, p2.GetType().FullName).Call();
            }
            catch (Exception ex)
            {
                SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolException, ex.ToString());
            }
            return Zeze.Transaction.Procedure.Excption;
        }

        public override int ProcessLinkBroken(LinkBroken protocol)
        {
            // 目前仅需设置online状态。
            if (protocol.Argument.States.Count > 0)
            {
                var roleId = protocol.Argument.States[0];
                Game.App.Instance.Game_Login.Onlines.OnLinkBroken(roleId);
            }
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessModuleRedirectRequest(ModuleRedirect rpc)
        {
            try
            {
                if (false == Game.ModuleRedirect.Instance.Handles.TryGetValue(rpc.Argument.MethodFullName, out var handle))
                {
                    rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
                    return Zeze.Transaction.Procedure.LogicError;
                }
                rpc.Result.ReturnCode = handle(rpc);
                rpc.Result.ModuleId = rpc.Argument.ModuleId;
                rpc.Result.AutoKeyLocalId = App.Zeze.Config.AutoKeyLocalId;
                rpc.SendResultCode(ModuleRedirect.ResultCodeSuccess);
                return rpc.Result.ReturnCode;
            }
            catch (Exception ex)
            {
                logger.Error(ex);
                rpc.SendResultCode(ModuleRedirect.ResultCodeHandleException);
                return Zeze.Transaction.Procedure.Excption;
            }
        }
    }
}
