using System;
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

            App.Instance.Scheduler.Schedule(() =>
            {
                App.Instance.Zeze.NewProcedure(() =>
                {
                    // 网络断开后延迟删除在线状态。这里简单判断一下是否StateNetBroken。
                    // 由于CLogin,CReLogin的时候没有取消Timeout，所以有可能再次登录断线后，会被上一次断线的Timeout删除。
                    // 造成延迟时间不准确。管理Timeout有点烦，先这样吧。
                    var online = table.Get(roleId);
                    if (null != online && online.State == BOnline.StateNetBroken)
                        table.Remove(roleId);
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
            // ExecutorOneByOne 使用 listenerName 当作 key 更准确。相同的数据挨个执行。
            // 使用 TypeId 效率高些，也可以，虽然协议可能被多个数据共用，但逻辑上也是正确的。
            // 不同的数据(协议)可以并发执行。
            Game.App.Instance.Zeze.ExecutorOneByOne.Execute(typeId, Game.App.Instance.Zeze.NewProcedure(() =>
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
            }, "SendReliableNotify." + listenerName).Call);
        }

        class ToLink
        {
            public Zeze.Net.AsyncSocket Socket { get; }
            public gnet.Provider.Send Protocol { get; } = new gnet.Provider.Send();

            public ToLink(AsyncSocket socket)
            {
                Socket = socket;
            }
        }

        private void SendInProcedure(ICollection<long> roleIds, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            Dictionary<string, ToLink> toLinks = new Dictionary<string, ToLink>();
            foreach (var roleId in roleIds)
            {
                var online = table.Get(roleId);
                if (null == online || online.State == BOnline.StateOffline)
                    continue;

                if (false == Game.App.Instance.Server.Links.TryGetValue(online.LinkName, out var socket))
                    continue;

                if (false == toLinks.TryGetValue(online.LinkName, out var toLink))
                {
                    toLink = new ToLink(socket);
                    toLink.Protocol.Argument.ProtocolType = typeId;
                    toLink.Protocol.Argument.ProtocolWholeData = fullEncodedProtocol;
                    toLinks.Add(online.LinkName, toLink);
                }
                toLink.Protocol.Argument.LinkSids.Add(online.LinkSid);
            }

            foreach (var toLink in toLinks.Values)
            {
                toLink.Socket.Send(toLink.Protocol);
            }
        }

        private void Send(ICollection<long> roleIds, int typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            Task.Run(Game.App.Instance.Zeze.NewProcedure(() =>
            {
                SendInProcedure(roleIds, typeId, fullEncodedProtocol);
                return Zeze.Transaction.Procedure.Success;
            }, "Onlines.Send").Call);
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
    }
}
