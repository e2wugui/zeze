
using System;
using System.Collections.Generic;
using static Zeze.Net.Service;
using Zeze.Transaction;
using Zeze.Net;

namespace Zezex.Provider
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

        public override long ProcessDispatch(Protocol _p)
        {
            var p = _p as Dispatch;
            try
            {
                var factoryHandle = Game.App.Instance.Server.FindProtocolFactoryHandle(p.Argument.ProtocolType);
                if (null == factoryHandle)
                {
                    SendKick(p.Sender, p.Argument.LinkSid, BKick.ErrorProtocolUnkown, "unknown protocol");
                    return Procedure.LogicError;
                }
                var p2 = factoryHandle.Factory();
                p2.Service = p.Service;
                p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.ProtocolData));
                p2.Sender = p.Sender;
                
                var session = new Game.Login.Session(
                    p.Argument.Account,
                    p.Argument.States,
                    p.Sender,
                    p.Argument.LinkSid);

                p2.UserState = session;
                if (Transaction.Current != null)
                {
                    // 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
                    Transaction.Current.TopProcedure.ActionName = p2.GetType().FullName;
                    Transaction.Current.TopProcedure.UserState = p2.UserState;
                    return Zeze.Util.Task.Call(
                        () => factoryHandle.Handle(p2),
                        p2,
                        (p, code) => { p.ResultCode = code; session.SendResponse(p); });
                }

                if (p2.Sender.Service.Zeze == null || factoryHandle.NoProcedure)
                {
                    // 应用框架不支持事务或者协议配置了“不需要事务”
                    return Zeze.Util.Task.Call(
                        () => factoryHandle.Handle(p2),
                        p2,
                        (p, code) => { p.ResultCode = code; session.SendResponse(p); });
                }

                // 创建存储过程并且在当前线程中调用。
                return Zeze.Util.Task.Call(
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

        public override long ProcessLinkBroken(Protocol p)
        {
            var protocol = p as LinkBroken;
            // 目前仅需设置online状态。
            if (protocol.Argument.States.Count > 0)
            {
                var roleId = protocol.Argument.States[0];
                Game.App.Instance.Game_Login.Onlines.OnLinkBroken(roleId);
            }
            return Procedure.Success;
        }

        public override long ProcessModuleRedirectRequest(Protocol p)
        {
            var rpc = p as ModuleRedirect;
            try
            {
                // replace RootProcedure.ActionName. 为了统计和日志输出。
                Transaction.Current.TopProcedure.ActionName = rpc.Argument.MethodFullName;

                rpc.Result.ModuleId = rpc.Argument.ModuleId;
                rpc.Result.ServerId = App.Zeze.Config.ServerId;
                if (false == Zezex.ModuleRedirect.Instance.Handles.TryGetValue(rpc.Argument.MethodFullName, out var handle))
                {
                    rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
                    return Procedure.LogicError;
                }
                var (ReturnCode, Params) = handle(rpc.SessionId, rpc.Argument.HashCode, rpc.Argument.Params, rpc.Result.Actions);
                rpc.Result.ReturnCode = ReturnCode;
                if (ReturnCode == Procedure.Success)
                {
                    rpc.Result.Params = Params;
                }
                // rpc 成功了，具体handle结果还需要看ReturnCode。
                rpc.SendResultCode(ModuleRedirect.ResultCodeSuccess);
                return rpc.Result.ReturnCode;
            }
            catch (Exception)
            {
                rpc.SendResultCode(ModuleRedirect.ResultCodeHandleException);
                throw;
            }
        }

        private void SendResultIfSizeExceed(Zeze.Net.AsyncSocket sender, ModuleRedirectAllResult result)
        {
            int size = 0;
            foreach (var hashResult in result.Argument.Hashs.Values)
            {
                size += hashResult.Params.Count;
                foreach (var hashActions in hashResult.Actions)
                {
                    size += hashActions.Params.Count;
                }
            }
            if (size > 2 * 1024 * 1024) // 2M
            {
                result.Send(sender);
                result.Argument.Hashs.Clear();
            }
        }

        public override long ProcessModuleRedirectAllRequest(Protocol p)
        {
            var protocol = p as ModuleRedirectAllRequest;
            var result = new ModuleRedirectAllResult();
            try
            {
                // replace RootProcedure.ActionName. 为了统计和日志输出。
                Transaction.Current.TopProcedure.ActionName = protocol.Argument.MethodFullName;

                // common parameters for result
                result.Argument.ModuleId = protocol.Argument.ModuleId;
                result.Argument.ServerId = App.Zeze.Config.ServerId;
                result.Argument.SourceProvider = protocol.Argument.SourceProvider;
                result.Argument.SessionId = protocol.Argument.SessionId;
                result.Argument.MethodFullName = protocol.Argument.MethodFullName;

                if (false == Zezex.ModuleRedirect.Instance.Handles.TryGetValue(
                    protocol.Argument.MethodFullName, out var handle))
                {
                    result.ResultCode = ModuleRedirect.ResultCodeMethodFullNameNotFound;
                    // 失败了，需要把hash返回。此时是没有处理结果的。
                    foreach (var hash in protocol.Argument.HashCodes)
                    {
                        result.Argument.Hashs.Add(hash, new BModuleRedirectAllHash()
                        {
                            ReturnCode = Procedure.NotImplement
                        });
                    }
                    result.Send(protocol.Sender);
                    return Procedure.LogicError;
                }
                result.ResultCode = ModuleRedirect.ResultCodeSuccess;

                foreach (var hash in protocol.Argument.HashCodes)
                {
                    // 嵌套存储过程，某个分组处理失败不影响其他分组。
                    var hashResult = new BModuleRedirectAllHash();
                    Zeze.Net.Binary Params = null;
                    hashResult.ReturnCode = App.Zeze.NewProcedure(() =>
                    {
                        var (_ReturnCode, _Params) = handle(protocol.Argument.SessionId, hash, protocol.Argument.Params, hashResult.Actions);
                        Params = _Params;
                        return _ReturnCode;
                    }, Transaction.Current.TopProcedure.ActionName).Call();

                    // 单个分组处理失败继续执行。XXX
                    if (hashResult.ReturnCode == Procedure.Success)
                    {
                        hashResult.Params = Params;
                    }
                    result.Argument.Hashs.Add(hash, hashResult);
                    SendResultIfSizeExceed(protocol.Sender, result);
                }

                // send remain
                if (result.Argument.Hashs.Count > 0)
                {
                    result.Send(protocol.Sender);
                }
                return Procedure.Success;
            }
            catch (Exception)
            {
                result.ResultCode = ModuleRedirect.ResultCodeHandleException;
                result.Send(protocol.Sender);
                throw;
            }
        }

        public class ModuleRedirectAllContext : Zeze.Net.Service.ManualContext
        {
            public string MethodFullName { get; }
            public HashSet<int> HashCodes { get; } = new HashSet<int>();
            public Action<ModuleRedirectAllContext> OnHashEnd { get; set; }

            public ModuleRedirectAllContext(int concurrentLevel, string methodFullName)
            {
                for (int hash = 0; hash < concurrentLevel; ++hash)
                    HashCodes.Add(hash);
                MethodFullName = methodFullName;
            }

            public override void OnRemoved()
            {
                lock (this)
                {
                    OnHashEnd?.Invoke(this);
                    OnHashEnd = null;
                }
            }

            /// <summary>
            /// 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
            /// 1) 在锁内执行；
            /// 2) 需要时初始化UserState并传给action；
            /// 3) 处理完成时删除Context
            /// </summary>
            public long ProcessHash<T>(int hash, Func<T> factory, Func<T, long> action)
            {
                lock (this)
                {
                    try
                    {
                        if (null == UserState)
                            UserState = factory();
                        return action((T)UserState);
                    }
                    finally
                    {
                        HashCodes.Remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
                        if (HashCodes.Count == 0)
                        {
                            Game.App.Instance.Server.TryRemoveManualContext<ManualContext>(SessionId);
                        }
                    }
                }
            }

            // 这里处理真正redirect发生时，从远程返回的结果。
            public void ProcessResult(ModuleRedirectAllResult result)
            {
                foreach (var h in result.Argument.Hashs)
                {
                    // 嵌套存储过程，单个分组的结果处理不影响其他分组。
                    // 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
                    Game.App.Instance.Zeze.NewProcedure(() =>
                        ProcessHashResult(h.Key, h.Value.ReturnCode, h.Value.Params, h.Value.Actions),
                        MethodFullName).Call();
                }
            }

            // 生成代码实现。see Game.ModuleRedirect.cs
            public virtual long ProcessHashResult(
                int _hash_,
                long _returnCode_,
                Zeze.Net.Binary _params,
                IList<Zezex.Provider.BActionParam> _actions_)
            {
                return Procedure.NotImplement;
            }
        }

        public override long ProcessModuleRedirectAllResult(Protocol p)
        {
            var protocol = p as ModuleRedirectAllResult;
            // replace RootProcedure.ActionName. 为了统计和日志输出。
            Transaction.Current.TopProcedure.ActionName = protocol.Argument.MethodFullName;
            App.Server.TryGetManualContext<ModuleRedirectAllContext>(
                protocol.Argument.SessionId)?.ProcessResult(protocol);
            return Procedure.Success;
        }

        public override long ProcessTransmit(Protocol p)
        {
            var protocol = p as Transmit;
            App.Game_Login.Onlines.ProcessTransmit(protocol.Argument.Sender,
                protocol.Argument.ActionName, protocol.Argument.Roles.Keys);
            return Procedure.Success;
        }

        public override long ProcessAnnounceLinkInfo(Protocol p)
        {
            var protocol = p as AnnounceLinkInfo;
            var linkSession = protocol.Sender.UserState as Game.Server.LinkSession;
            linkSession.Setup(protocol.Argument.LinkId, protocol.Argument.ProviderSessionId);
            return Procedure.Success;
        }

        public override long ProcessSendConfirm(Protocol p)
        {
            var protocol = p as SendConfirm;
            var linkSession = protocol.Sender.UserState as Game.Server.LinkSession;
            App.Server.TryGetManualContext<Game.Login.Onlines.ConfirmContext>(
                protocol.Argument.ConfirmSerialId)?.ProcessLinkConfirm(linkSession.Name);
            // linkName 也可以从 protocol.Sender.Connector.Name 获取。
            return Procedure.Success;
        }
    }
}
