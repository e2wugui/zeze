using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Serialize;

namespace Game.Login
{
    public class Onlines
    {
        private tonline table;

        public Onlines(tonline table)
        {
            this.table = table;
        }

        public void OnLinkBroken(long roleId)
        {
            var online = table.Get(roleId);
            if (null != online)
                online.State = BOnline.StateNetBroken;

            global::Zeze.Util.Scheduler.Instance.Schedule((ThisTask) =>
            {
                App.Instance.Zeze.NewProcedure(() =>
                {
                    // 网络断开后延迟删除在线状态。这里简单判断一下是否StateNetBroken。
                    // 由于CLogin,CReLogin的时候没有取消Timeout，所以有可能再次登录断线后，会被上一次断线的Timeout删除。
                    // 造成延迟时间不准确。管理Timeout有点烦，先这样吧。
                    var online = table.Get(roleId);
                    if (null != online && online.State == BOnline.StateNetBroken)
                        table.Remove(roleId);
                    App.Instance.Load.LogoutCount.IncrementAndGet();

                    return Procedure.Success;
                }, "Onlines.OnLinkBroken").Call();
            }, 10 * 60 * 1000); // 10 minuts for relogin
        }

        public void AddReliableNotifyMark(long roleId, string listenerName)
        {
            var online = table.Get(roleId);
            if (null == online || online.State != BOnline.StateOnline)
                throw new Exception("Not Online. AddReliableNotifyMark: " + listenerName);
            online.ReliableNotifyMark.Add(listenerName);
        }

        public void RemoveReliableNotifyMark(long roleId, string listenerName)
        {
            // 移除尽量通过，不做任何判断。
            table.Get(roleId)?.ReliableNotifyMark.Remove(listenerName);
        }

        public void SendReliableNotifyWhileCommit(
            long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
        {
            Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(roleId, listenerName, p, WaitConfirm)
                );
        }

        public void SendReliableNotifyWhileCommit(
            long roleId, string listenerName, int typeId, Binary fullEncodedProtocol,
            bool WaitConfirm = false)
        {
            Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm)
                );
        }

        public void SendReliableNotifyWhileRollback(
            long roleId, string listenerName, Protocol p,
            bool WaitConfirm = false)
        {
            Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(roleId, listenerName, p, WaitConfirm)
                );
        }

        public void SendReliableNotifyWhileRollback(
            long roleId, string listenerName, int typeId, Binary fullEncodedProtocol,
            bool WaitConfirm = false)
        {
            Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm)
                );
        }

        public void SendReliableNotify(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
        {
            SendReliableNotify(roleId, listenerName, p.TypeId, new Binary(p.Encode()), WaitConfirm);
        }

        /// <summary>
        /// 发送在线可靠协议，如果不在线等，仍然不会发送哦。
        /// </summary>
        /// <param name="roleId"></param>
        /// <param name="listenerName"></param>
        /// <param name="fullEncodedProtocol">协议必须先编码，因为会跨事务。</param>
        public void SendReliableNotify(
            long roleId, string listenerName, long typeId, Binary fullEncodedProtocol,
            bool WaitConfirm = false)
        {
            TaskCompletionSource<long> future = null;

            if (WaitConfirm)
                future = new TaskCompletionSource<long>();

            App.Instance.Zeze.TaskOneByOneByKey.Execute(
                listenerName,
                App.Instance.Zeze.NewProcedure(() =>
                {
                    BOnline online = table.Get(roleId);
                    if (null == online || online.State == BOnline.StateOffline)
                    {
                        return Procedure.Success;
                    }
                    if (false == online.ReliableNotifyMark.Contains(listenerName))
                    {
                        return Procedure.Success; // 相关数据装载的时候要同步设置这个。
                    }

                    // 先保存在再发送，然后客户端还会确认。
                    // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
                    online.ReliableNotifyQueue.Add(fullEncodedProtocol);
                    if (online.State == BOnline.StateOnline)
                    {
                        var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
                        notify.Argument.ReliableNotifyTotalCountStart = online.ReliableNotifyTotalCount;
                        notify.Argument.Notifies.Add(fullEncodedProtocol);

                        SendInProcedure(roleId, notify.TypeId, new Binary(notify.Encode()), future);
                    }
                    online.ReliableNotifyTotalCount += 1; // 后加，start 是 Queue.Add 之前的。
                    return Procedure.Success;
                },
                "SendReliableNotify." + listenerName
                ));

            future?.Task.Wait();
        }

        public class RoleOnLink
        {
            public string LinkName { get; set; } = ""; // empty when not online
            public AsyncSocket LinkSocket { get; set; } // null if not online
            public int ProviderId { get; set; } = -1;
            public long ProviderSessionId { get; set; }
            public Dictionary<long, Zezex.Provider.BTransmitContext> Roles { get; }
                = new Dictionary<long, Zezex.Provider.BTransmitContext>();
        }

        public ICollection<RoleOnLink> GroupByLink(ICollection<long> roleIds)
        {
            var groups = new Dictionary<string, RoleOnLink>();
            var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.LinkName, groupNotOnline);

            foreach (var roleId in roleIds)
            {
                var online = table.Get(roleId);
                if (null == online || online.State != BOnline.StateOnline)
                {
                    groupNotOnline.Roles.TryAdd(roleId, new Zezex.Provider.BTransmitContext());
                    continue;
                }

                if (false == Game.App.Instance.Server.Links.TryGetValue(online.LinkName, out var connector))
                {
                    groupNotOnline.Roles.TryAdd(roleId, new Zezex.Provider.BTransmitContext());
                    continue;
                }

                if (false == connector.IsHandshakeDone)
                {
                    groupNotOnline.Roles.TryAdd(roleId, new Zezex.Provider.BTransmitContext());
                    continue;
                }
                // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
                if (false == groups.TryGetValue(online.LinkName, out var group))
                {
                    group = new RoleOnLink()
                    {
                        LinkName = online.LinkName,
                        LinkSocket = connector.Socket,
                        ProviderId = online.ProviderId,
                        ProviderSessionId = online.ProviderSessionId,
                    };
                    groups.Add(group.LinkName, group);
                }
                group.Roles.TryAdd(roleId, new Zezex.Provider.BTransmitContext()
                {
                    LinkSid = online.LinkSid,
                    ProviderId = online.ProviderId,
                    ProviderSessionId = online.ProviderSessionId,
                }); // 使用 TryAdd，忽略重复的 roleId。
            }
            return groups.Values;
        }

        private void SendInProcedure(
            long roleId, long typeId, Binary fullEncodedProtocol,
            TaskCompletionSource<long> future)
        {
            // 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
            var groups = GroupByLink(new List<long> { roleId });
            long serialId = 0;
            if (null != future)
            {
                var confrmContext = new ConfirmContext(future);
                // 必须在真正发送前全部加入，否则要是发生结果很快返回，
                // 导致异步问题：错误的认为所有 Confirm 都收到。
                foreach (var group in groups)
                {
                    if (group.LinkSocket == null)
                        continue; // skip not online

                    confrmContext.LinkNames.Add(group.LinkName);
                }
                serialId = App.Instance.Server.AddManualContextWithTimeout(confrmContext, 5000);
            }

            foreach (var group in groups)
            {
                if (group.LinkSocket == null)
                    continue; // skip not online

                var send = new Zezex.Provider.Send();
                send.Argument.ProtocolType = typeId;
                send.Argument.ProtocolWholeData = fullEncodedProtocol;
                send.Argument.ConfirmSerialId = serialId;

                foreach (var ctx in group.Roles.Values)
                {
                    send.Argument.LinkSids.Add(ctx.LinkSid);
                }
                group.LinkSocket.Send(send);
            }
        }

        private void Send(
            long roleId, long typeId, Binary fullEncodedProtocol,
            bool WaitConfirm)
        {
            TaskCompletionSource<long> future = null;

            if (WaitConfirm)
                future = new TaskCompletionSource<long>();

            // 发送协议请求在另外的事务中执行。
            Game.App.Instance.Zeze.TaskOneByOneByKey.Execute(roleId, () =>
                Zeze.Util.Task.Call(Game.App.Instance.Zeze.NewProcedure(() =>
                {
                    SendInProcedure(roleId, typeId, fullEncodedProtocol, future);
                    return Procedure.Success;
                }, "Onlines.Send")));

            future?.Task.Wait();
        }

        public void Send(long roleId, Protocol p, bool WaitConfirm = false)
        {
            Send(roleId, p.TypeId, new Binary(p.Encode()), WaitConfirm);
        }

        public void Send(ICollection<long> roleIds, Protocol p)
        {
            foreach (var roleId in roleIds)
                Send(roleId, p.TypeId, new Binary(p.Encode()), false);
        }

        public void SendWhileCommit(long roleId, Protocol p, bool WaitConfirm = false)
        {
            Transaction.Current.RunWhileCommit(() => Send(roleId, p, WaitConfirm));
        }

        public void SendWhileCommit(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => Send(roleIds, p));
        }

        public void SendWhileRollback(long roleId, Protocol p, bool WaitConfirm = false)
        {
            Transaction.Current.RunWhileRollback(() => Send(roleId, p, WaitConfirm));
        }

        public void SendWhileRollback(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Current.RunWhileRollback(() => Send(roleIds, p));
        }

        /// <summary>
        /// Func<sender, target, result>
        /// sender: 查询发起者，结果发送给他。
        /// target: 查询目标角色。
        /// result: 返回值，int，按普通事务处理过程返回值处理。
        /// </summary>
        public ConcurrentDictionary<string, Func<long, long, Serializable, long>> TransmitActions { get; }
            = new ConcurrentDictionary<string, Func<long, long, Serializable, long>>();

        public ConcurrentDictionary<string, Func<string, Serializable>> TransmitParameterFactorys { get; }
            = new ConcurrentDictionary<string, Func<string, Serializable>>();

        /// <summary>
        /// 转发查询请求给RoleId。
        /// </summary>
        /// <param name="sender">查询发起者，结果发送给他。</param>
        /// <param name="actionName">查询处理的实现</param>
        /// <param name="roleId">目标角色</param>
        public void Transmit(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            Transmit(sender, actionName, new List<long>() { roleId }, parameter);
        }

        public void ProcessTransmit(long sender, string actionName, IEnumerable<long> roleIds, Serializable parameter)
        {
            if (TransmitActions.TryGetValue(actionName, out var handle))
            {
                foreach (var target in roleIds)
                {
                    Zeze.Util.Task.Run(App.Instance.Zeze.NewProcedure(
                        () => handle(sender, target, parameter), "Game.Online.Transmit:" + actionName));
                }
            }
        }

        private void TransmitInProcedure(long sender, string actionName, ICollection<long> roleIds, Serializable parameter)
        {
            if (App.Instance.Zeze.Config.GlobalCacheManagerHostNameOrAddress.Length == 0)
            {
                // 没有启用cache-sync，马上触发本地任务。
                ProcessTransmit(sender, actionName, roleIds, parameter);
                return;
            }

            var groups = GroupByLink(roleIds);
            foreach (var group in groups)
            {
                if (group.ProviderId == App.Instance.Zeze.Config.ServerId
                    || null == group.LinkSocket // 对于不在线的角色，直接在本机运行。
                    )
                {
                    // loopback 就是当前gs.
                    ProcessTransmit(sender, actionName, group.Roles.Keys, parameter);
                    continue;
                }

                var transmit = new Zezex.Provider.Transmit();

                transmit.Argument.ActionName = actionName;
                transmit.Argument.Sender = sender;
                transmit.Argument.ServiceNamePrefix = App.ServerServiceNamePrefix;
                transmit.Argument.Roles.AddRange(group.Roles);

                if (null != parameter)
                {
                    transmit.Argument.ParameterBeanName = parameter.GetType().FullName;
                    transmit.Argument.ParameterBeanValue = new Binary(Zeze.Serialize.ByteBuffer.Encode(parameter));
                }

                group.LinkSocket.Send(transmit);
            }
        }

        public void Transmit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);

            // 发送协议请求在另外的事务中执行。
            Zeze.Util.Task.Run(Game.App.Instance.Zeze.NewProcedure(() =>
            {
                TransmitInProcedure(sender, actionName, roleIds, parameter);
                return Procedure.Success;
            }, "Onlines.Transmit"));
        }

        public void TransmitWhileCommit(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileCommit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleIds, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleIds, parameter));
        }

        public class ConfirmContext : Service.ManualContext
        {
            public HashSet<string> LinkNames { get; } = new HashSet<string>();
            public TaskCompletionSource<long> Future { get; }

            public ConfirmContext(TaskCompletionSource<long> future)
            {
                Future = future;
            }

            public override void OnRemoved()
            {
                lock (this)
                {
                    Future.SetResult(base.SessionId);
                }
            }

            public long ProcessLinkConfirm(string linkName)
            {
                lock (this)
                {
                    LinkNames.Remove(linkName);
                    if (LinkNames.Count == 0)
                    {
                        App.Instance.Server.TryRemoveManualContext<ConfirmContext>(SessionId);
                    }
                    return Procedure.Success;
                }
            }
        }

        private void Broadcast(long typeId, Binary fullEncodedProtocol, int time, bool WaitConfirm)
        {
            TaskCompletionSource<long> future = null;
            long serialId = 0;
            if (WaitConfirm)
            {
                future = new TaskCompletionSource<long>();
                var confirmContext = new ConfirmContext(future);
                foreach (var link in App.Instance.Server.Links.Values)
                {
                    if (link.Socket != null)
                        confirmContext.LinkNames.Add(link.Name);
                }
                serialId = App.Instance.Server.AddManualContextWithTimeout(confirmContext, 5000);
            }

            var broadcast = new Zezex.Provider.Broadcast();
            broadcast.Argument.ProtocolType = typeId;
            broadcast.Argument.ProtocolWholeData = fullEncodedProtocol;
            broadcast.Argument.ConfirmSerialId = serialId;
            broadcast.Argument.Time = time;

            foreach (var link in App.Instance.Server.Links.Values)
            {
                link.Socket?.Send(broadcast);
            }

            future?.Task.Wait();
        }

        public void Broadcast(Protocol p, int time = 60 * 1000, bool WaitConfirm = false)
        {
            Broadcast(p.TypeId, new Binary(p.Encode()), time, WaitConfirm);
        }
    }
}
