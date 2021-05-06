using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;

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

            App.Instance.Scheduler.Schedule((ThisTask) =>
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

        public void SendReliableNotifyWhileCommit(long roleId, string listenerName, Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => SendReliableNotify(roleId, listenerName, p));
        }

        public void SendReliableNotifyWhileCommit(long roleId, string listenerName, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            Transaction.Current.RunWhileCommit(() => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
        }

        public void SendReliableNotifyWhileRollback(long roleId, string listenerName, Protocol p)
        {
            Transaction.Current.RunWhileRollback(() => SendReliableNotify(roleId, listenerName, p));
        }

        public void SendReliableNotifyWhileRollback(long roleId, string listenerName, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            Transaction.Current.RunWhileRollback(() => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
        }

        public void SendReliableNotify(long roleId, string listenerName, Protocol p)
        {
            SendReliableNotify(roleId, listenerName, p.TypeId, new Zeze.Net.Binary(p.Encode()));
        }

        /// <summary>
        /// 发送在线可靠协议，如果不在线等，仍然不会发送哦。
        /// </summary>
        /// <param name="roleId"></param>
        /// <param name="listenerName"></param>
        /// <param name="fullEncodedProtocol">协议必须先编码，因为会跨事务。</param>
        public void SendReliableNotify(long roleId, string listenerName, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            Game.App.Instance.Zeze.TaskOneByOneByKey.Execute(listenerName, Game.App.Instance.Zeze.NewProcedure(() =>
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

                    SendInProcedure(new List<long>() { roleId }, notify.TypeId, new Zeze.Net.Binary(notify.Encode()));
                }
                online.ReliableNotifyTotalCount += 1; // 后加，start 是 Queue.Add 之前的。
                return Zeze.Transaction.Procedure.Success;
            }, "SendReliableNotify." + listenerName));
        }

        public class RoleOnLink
        {
            public string LinkName { get; set; } = ""; // empty when not online
            public AsyncSocket LinkSocket { get; set; } // null if not online
            public int ProviderId { get; set; } = -1;
            public long ProviderSessionId { get; set; }
            public Dictionary<long, gnet.Provider.BTransmitContext> Roles { get; } = new Dictionary<long, gnet.Provider.BTransmitContext>();
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
                    groupNotOnline.Roles.TryAdd(roleId, new gnet.Provider.BTransmitContext());
                    continue;
                }

                if (false == Game.App.Instance.Server.Links.TryGetValue(online.LinkName, out var connector))
                {
                    groupNotOnline.Roles.TryAdd(roleId, new gnet.Provider.BTransmitContext());
                    continue;
                }

                if (false == connector.IsHandshakeDone)
                {
                    groupNotOnline.Roles.TryAdd(roleId, new gnet.Provider.BTransmitContext());
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
                group.Roles.TryAdd(roleId, new gnet.Provider.BTransmitContext()
                {
                    LinkSid = online.LinkSid,
                    ProviderId = online.ProviderId,
                    ProviderSessionId = online.ProviderSessionId,
                }); // 使用 TryAdd，忽略重复的 roleId。
            }
            return groups.Values;
        }

        private void SendInProcedure(ICollection<long> roleIds, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            var groups = GroupByLink(roleIds);
            foreach (var group in groups)
            {
                if (group.LinkSocket == null)
                    continue; // skip not online

                var send = new gnet.Provider.Send();
                send.Argument.ProtocolType = typeId;
                send.Argument.ProtocolWholeData = fullEncodedProtocol;
                foreach (var ctx in group.Roles.Values)
                {
                    send.Argument.LinkSids.Add(ctx.LinkSid);
                }
                group.LinkSocket.Send(send);
            }
        }

        private void Send(ICollection<long> roleIds, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            // 发送协议请求在另外的事务中执行。
            Zeze.Util.Task.Run(Game.App.Instance.Zeze.NewProcedure(() =>
            {
                SendInProcedure(roleIds, typeId, fullEncodedProtocol);
                return Zeze.Transaction.Procedure.Success;
            }, "Onlines.Send"));
        }

        public void Send(long roleId, Protocol p)
        {
            Send(new List<long>() { roleId }, p.TypeId, new Zeze.Net.Binary(p.Encode()));
        }

        public void Send(ICollection<long> roleIds, Protocol p)
        {
            Send(roleIds, p.TypeId, new Zeze.Net.Binary(p.Encode()));
        }

        public void SendWhileCommit(long roleId, Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => Send(roleId, p));
        }

        public void SendWhileCommit(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => Send(roleIds, p));
        }

        public void SendWhileRollback(long roleId, Protocol p)
        {
            Transaction.Current.RunWhileRollback(() => Send(roleId, p));
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
        public ConcurrentDictionary<string, Func<long, long, int>> TransmitActions { get; }
            = new ConcurrentDictionary<string, Func<long, long, int>>();

        /// <summary>
        /// 转发查询请求给RoleId。
        /// </summary>
        /// <param name="sender">查询发起者，结果发送给他。</param>
        /// <param name="actionName">查询处理的实现</param>
        /// <param name="roleId">目标角色</param>
        public void Transmit(long sender, string actionName, long roleId)
        {
            Transmit(sender, actionName, new List<long>() { roleId });
        }

        public void ProcessTransmit(long sender, string actionName, IEnumerable<long> roleIds)
        {
            if (TransmitActions.TryGetValue(actionName, out var handle))
            {
                foreach (var target in roleIds)
                {
                    Zeze.Util.Task.Run(App.Instance.Zeze.NewProcedure(
                        () => handle(sender, target), "Game.Online.Transmit:" + actionName));
                }
            }
        }

        private void TransmitInProcedure(long sender, string actionName, ICollection<long> roleIds)
        {
            if (App.Instance.Zeze.Config.GlobalCacheManagerHostNameOrAddress.Length == 0)
            {
                // 没有启用cache-sync，马上触发本地任务。
                ProcessTransmit(sender, actionName, roleIds);
                return;
            }

            var groups = GroupByLink(roleIds);
            foreach (var group in groups)
            {
                if (group.ProviderId == App.Instance.Zeze.Config.AutoKeyLocalId)
                {
                    // loopback 就是当前gs.
                    ProcessTransmit(sender, actionName, group.Roles.Keys);
                    continue;
                }
                var transmit = new gnet.Provider.Transmit();
                transmit.Argument.ActionName = actionName;
                transmit.Argument.Sender = sender;
                transmit.Argument.Roles.AddRange(group.Roles);

                if (null != group.LinkSocket)
                {
                    group.LinkSocket.Send(transmit);
                    continue;
                }

                // 对于不在线的角色，随机选择一个linkd转发。
                List<AsyncSocket> readyLinks = new List<AsyncSocket>();
                foreach (var link in App.Instance.Server.Links.Values)
                {
                    if (link.IsHandshakeDone)
                    {
                        readyLinks.Add(link.Socket);
                    }
                }
                if (readyLinks.Count > 0)
                {
                    var randLink = readyLinks[Zeze.Util.Random.Instance.Next(readyLinks.Count)];
                    randLink.Send(transmit);
                }
            }
        }

        public void Transmit(long sender, string actionName, ICollection<long> roleIds)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);

            // 发送协议请求在另外的事务中执行。
            Zeze.Util.Task.Run(Game.App.Instance.Zeze.NewProcedure(() =>
            {
                TransmitInProcedure(sender, actionName, roleIds);
                return Zeze.Transaction.Procedure.Success;
            }, "Onlines.Transmit"));
        }

        public void TransmitWhileCommit(long sender, string actionName, long roleId)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleId));
        }

        public void TransmitWhileCommit(long sender, string actionName, ICollection<long> roleIds)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleIds));
        }

        public void TransmitWhileRollback(long sender, string actionName, long roleId)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleId));
        }

        public void TransmitWhileRollback(long sender, string actionName, ICollection<long> roleIds)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleIds));
        }
    }
}
