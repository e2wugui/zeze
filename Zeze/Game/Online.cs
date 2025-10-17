using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Arch.Gen;
using Zeze.Builtin.Game.Online;
using Zeze.Builtin.Provider;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Game
{
    public class Online : AbstractOnline
    {
        public static long GetSpecialTypeIdFromBean(Bean bean)
        {
            return bean.TypeId;
        }

        public static Bean CreateBeanFromSpecialTypeId(long typeId)
        {
            throw new InvalidOperationException("Online Memory Table Dynamic Only.");
        }

        private static readonly ILogger logger = LogManager.GetLogger(typeof(Online));

        public ProviderApp ProviderApp { get; }
        public AppBase App { get; }
        public ProviderLoad LoadReporter { get; }

        public static Online Create(AppBase app)
        {
            return GenModule.CreateRedirectModule(app, new Online());
        }

        internal Online()
        { 
            // for gen
        }

        protected Online(AppBase app)
        {
            if (app == null)
                throw new ArgumentException("app is null");
            this.App = app;
            this.ProviderApp = app.Zeze.Redirect.ProviderApp;

            LoadReporter = new(this);
        }

        public override void Register()
        {
            RegisterProtocols(ProviderApp.ProviderService);
            RegisterZezeTables(ProviderApp.Zeze);
        }

        public override void UnRegister()
        {
            UnRegisterZezeTables(ProviderApp.Zeze);
            UnRegisterProtocols(ProviderApp.ProviderService);
        }

        private Util.SchedulerTask VerifyLocalTimer;

        public void Start()
        {
            ProviderApp.BuiltinModules.Add(FullName, this);
            LoadReporter.Start();
            VerifyLocalTimer = Util.Scheduler.ScheduleAt(VerifyLocal, 3 + Util.Random.Instance.Next(3), 10); // at 3:10 - 6:10
        }

        public void Stop()
        {
            LoadReporter.Stop();
            VerifyLocalTimer?.Cancel();
        }


        public int LocalCount => _tLocal.Cache.DataMap.Count;

        public long WalkLocal(Func<long, BLocal, bool> walker)
        {
            return _tLocal.WalkCache(walker);
        }

        public async Task SetLocalBean(long roleId, string key, Bean bean)
        {
            var bLocal = await _tLocal.GetAsync(roleId);
            if (null == bLocal)
                throw new Exception("roleid not online. " + roleId);
            var bAny = new BAny();
            bAny.Any.Bean = bean;
            bLocal.Datas[key] = bAny;
        }

        public async Task<T> GetLocalBean<T>(long roleId, string key)
            where T : Bean
        {
            var bLocal = await _tLocal.GetAsync(roleId);
            if (null == bLocal)
                return null;
            if (!bLocal.Datas.TryGetValue(key, out var data))
                return null;
            return (T)data.Any.Bean;

        }

        public Zeze.Util.EventDispatcher LoginEvents { get; } = new("Online.Login");
        public Zeze.Util.EventDispatcher ReloginEvents { get; } = new("Online.Relogin");
        public Zeze.Util.EventDispatcher LogoutEvents { get; } = new("Online.Logout");
        public Zeze.Util.EventDispatcher LocalRemoveEvents { get; } = new("Online.Local.Remove");

        private readonly Util.AtomicLong _LoginTimes = new();

        public long LoginTimes => _LoginTimes.Get();
        private async Task RemoveLocalAndTrigger(long roleId)
        {
            var arg = new LocalRemoveEventArgument()
            {
                RoleId = roleId,
                LocalData = (await _tLocal.GetAsync(roleId)).Copy(),
            };

            await _tLocal.RemoveAsync(roleId); // remove first

            await LocalRemoveEvents.TriggerEmbed(this, arg);
            await LocalRemoveEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LocalRemoveEvents.TriggerThread(this, arg));
        }

        private async Task LogoutTriggerExtra(long roleId)
        {
            var arg = new LogoutEventArgument()
            {
                RoleId = roleId,
                OnlineData = (await _tOnline.GetAsync(roleId)).Copy(),
            };

            await LogoutEvents.TriggerEmbed(this, arg);
            await LogoutEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LogoutEvents.TriggerThread(this, arg));
        }

        private async Task LogoutTrigger(long roleId)
        {
            var arg = new LogoutEventArgument()
            {
                RoleId = roleId,
                OnlineData = (await _tOnline.GetAsync(roleId)).Copy(),
            };

            await _tOnline.RemoveAsync(roleId); // remove first

            await LogoutEvents.TriggerEmbed(this, arg);
            await LogoutEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LogoutEvents.TriggerThread(this, arg));
        }

        private async Task LoginTrigger(long roleId)
        {
            var arg = new LoginArgument()
            {
                RoleId = roleId,
            };

            await LoginEvents.TriggerEmbed(this, arg);
            await LoginEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LoginEvents.TriggerThread(this, arg));
            _LoginTimes.IncrementAndGet();
        }

        private async Task ReloginTrigger(long roleId)
        {
            var arg = new LoginArgument()
            {
                RoleId = roleId,
            };

            await ReloginEvents.TriggerEmbed(this, arg);
            await ReloginEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => ReloginEvents.TriggerThread(this, arg));
            _LoginTimes.IncrementAndGet();
        }

        public async Task OnLinkBroken(long roleId, string linkName, long linkSid)
        {
            long currentLoginVersion = 0;
            {
                var online = await _tOnline.GetAsync(roleId);
                // skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
                if (null == online || false == online.LinkName.Equals(linkName) || online.LinkSid != linkSid)
                    return;
                var version = await _tVersion.GetOrAddAsync(roleId);

                var local = await _tLocal.GetAsync(roleId);
                if (local == null)
                    return; // 不在本机登录。

                currentLoginVersion = local.LoginVersion;
                if (version.LoginVersion != currentLoginVersion)
                    await RemoveLocalAndTrigger(roleId); // 本机数据已经过时，马上删除。
            }
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                // TODO 实现Timer以后，这里改成使用它，利用它的持久化特性达到不丢失Logout的目的。
                Util.Scheduler.Schedule(async (ThisTask) =>
                {
                    // TryRemove
                    await ProviderApp.Zeze.NewProcedure(async () =>
                    {
                    // local online 独立判断version分别尝试删除。
                    var local = await _tLocal.GetAsync(roleId);
                        if (null != local && local.LoginVersion == currentLoginVersion)
                        {
                            await RemoveLocalAndTrigger(roleId);
                        }
                    // 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
                    var online = await _tOnline.GetAsync(roleId);
                        var version = await _tVersion.GetOrAddAsync(roleId);
                        if (null != online && version.LoginVersion == currentLoginVersion)
                        {
                            version.LogoutVersion = version.LoginVersion;
                            await LogoutTrigger(roleId);
                        }
                        return ResultCode.Success;
                    }, "Onlines.OnLinkBroken").CallAsync();
                }, ProviderApp.Zeze.Config.OnlineLogoutDelay);
            });
        }

        public async Task AddReliableNotifyMark(long roleId, string listenerName)
        {
            var online = await _tOnline.GetAsync(roleId);
            if (null == online)
                throw new Exception("Not Online. AddReliableNotifyMark: " + listenerName);
            (await _tVersion.GetOrAddAsync(roleId)).ReliableNotifyMark.Add(listenerName);
        }

        public async Task RemoveReliableNotifyMark(long roleId, string listenerName)
        {
            // 移除尽量通过，不做任何判断。
            (await _tVersion.GetOrAddAsync(roleId)).ReliableNotifyMark.Remove(listenerName);
        }

        public void SendReliableNotifyWhileCommit(
            long roleId, string listenerName, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(roleId, listenerName, p)
                );
        }

        public void SendReliableNotifyWhileCommit(long roleId, string listenerName, int typeId, Binary fullEncodedProtocol)
        {
            Transaction.Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol)
                );
        }

        public void SendReliableNotifyWhileRollback(long roleId, string listenerName, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => SendReliableNotify(roleId, listenerName, p));
        }

        public void SendReliableNotifyWhileRollback(
            long roleId, string listenerName, int typeId, Binary fullEncodedProtocol)
        {
            Transaction.Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol)
                );
        }

        public void SendReliableNotify(long roleId, string listenerName, Protocol p)
        {
            SendReliableNotify(roleId, listenerName, p.TypeId, new Binary(p.Encode()));
        }

        private Zeze.Collections.Queue<BNotify> OpenQueue(long roleId)
        {
            return ProviderApp.Zeze.Queues.Open<BNotify>("Zeze.Game.Online.ReliableNotifyQueue:" + roleId);
        }

        /// <summary>
        /// 发送在线可靠协议，如果不在线等，仍然不会发送哦。
        /// </summary>
        /// <param name="roleId"></param>
        /// <param name="listenerName"></param>
        /// <param name="fullEncodedProtocol">协议必须先编码，因为会跨事务。</param>
        public void SendReliableNotify(
                long roleId, string listenerName, long typeId, Binary fullEncodedProtocol)
        {
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(
                listenerName,
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    BOnline online = await _tOnline.GetAsync(roleId);
                    if (null == online)
                    {
                        // 完全离线，忽略可靠消息发送：可靠消息仅仅为在线提供服务，并不提供全局可靠消息。
                        return ResultCode.Success;
                    }
                    var version = await _tVersion.GetOrAddAsync(roleId);
                    if (false == version.ReliableNotifyMark.Contains(listenerName))
                    {
                        return ResultCode.Success; // 相关数据装载的时候要同步设置这个。
                    }

                    // 先保存在再发送，然后客户端还会确认。
                    // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
                    var queue = OpenQueue(roleId);
                    await queue.AddAsync(new BNotify() { FullEncodedProtocol = fullEncodedProtocol });

                    var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
                    notify.Argument.ReliableNotifyIndex = version.ReliableNotifyIndex;
                    version.ReliableNotifyIndex += 1; // after set notify.Argument
                    notify.Argument.Notifies.Add(fullEncodedProtocol);

                    await SendEmbed(new List<long> { roleId }, notify.TypeId, new Binary(notify.Encode()));
                    return ResultCode.Success;
                },
                "SendReliableNotify." + listenerName
                ));
        }

        public class RoleOnLink
        {
            public string LinkName { get; set; } = ""; // empty when not online
            public AsyncSocket LinkSocket { get; set; } // null if not online
            public int ServerId { get; set; } = -1;
            public long ProviderSessionId { get; set; }
            public Dictionary<long, long> Roles { get; } = new(); // roleid -> linksid
            public Dictionary<long, long> Contexts { get; } = new(); // linksid -> roleid
        }

        public async Task<ICollection<RoleOnLink>> GroupByLink(ICollection<long> roleIds)
        {
            var groups = new Dictionary<string, RoleOnLink>();
            var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.LinkName, groupNotOnline);

            foreach (var roleId in roleIds)
            {
                var online = await _tOnline.GetAsync(roleId);
                if (null == online)
                {
                    groupNotOnline.Roles.TryAdd(roleId, 0);
                    continue;
                }

                if (false == ProviderApp.ProviderService.Links.TryGetValue(online.LinkName, out var connector))
                {
                    groupNotOnline.Roles.TryAdd(roleId, 0);
                    continue;
                }

                if (false == connector.IsHandshakeDone)
                {
                    groupNotOnline.Roles.TryAdd(roleId, 0);
                    continue;
                }
                // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
                if (false == groups.TryGetValue(online.LinkName, out var group))
                {
                    group = new RoleOnLink()
                    {
                        LinkName = online.LinkName,
                        LinkSocket = connector.Socket,
                        ServerId = (await _tVersion.GetOrAddAsync(roleId)).ServerId,
                    };
                    groups.Add(group.LinkName, group);
                }
                group.Roles.TryAdd(roleId, online.LinkSid);
                group.Contexts.TryAdd(online.LinkSid, roleId);
            }
            return groups.Values;
        }

        private async Task<long> TriggerLinkBroken(string linkName, ICollection<long> errorSids, Dictionary<long, long> context)
        {
            foreach (var sid in errorSids)
            {
                if (context.TryGetValue(sid, out var roleId))
                    await OnLinkBroken(roleId, linkName, sid);
            }
            return 0;
        }

        public void Send(AsyncSocket to, Dictionary<long, long> context, Send send)
        {
            send.Send(to, async (rpc) => await TriggerLinkBroken(
                ProviderService.GetLinkName(to),
                send.IsTimeout ? send.Argument.LinkSids : send.Result.ErrorLinkSids,
                context));
        }

        public async Task SendEmbed(ICollection<long> roles, long typeId, Binary fullEncodedProtocol)
        {
            // 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
            var groups = await GroupByLink(roles);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                foreach (var group in groups)
                {
                    if (group.LinkSocket == null)
                        continue; // skip not online

                        var send = new Send();
                        send.Argument.ProtocolType = typeId;
                        send.Argument.ProtocolWholeData = fullEncodedProtocol;
                        foreach (var linkSid in group.Roles.Values)
                            send.Argument.LinkSids.Add(linkSid);
                        Send(group.LinkSocket, group.Contexts, send);
                }
            });
        }

        public void Send(long roleId, long typeId, Binary fullEncodedProtocol)
        {
            // 发送协议请求在另外的事务中执行。
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(roleId, () =>
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendEmbed(new List<long> { roleId }, typeId, fullEncodedProtocol);
                    return ResultCode.Success;
                }, "Onlines.Send"));
        }

        public void Send(ICollection<long> roles, long typeId, Binary fullEncodedProtocol)
        {

            // 发送协议请求在另外的事务中执行。
            if (roles.Count > 0)
            {
                ProviderApp.Zeze.TaskOneByOneByKey.ExecuteCyclicBarrier(roles,
                    ProviderApp.Zeze.NewProcedure(async () =>
                    {
                        await SendEmbed(roles, typeId, fullEncodedProtocol);
                        return ResultCode.Success;
                    }, "Onlines.Send"));
            }
        }

        public void Send(long roleId, Protocol p)
        {
            Send(roleId, p.TypeId, new Binary(p.Encode()));
        }

        public void Send(ICollection<long> roleIds, Protocol p)
        {
            Send(roleIds, p.TypeId, new Binary(p.Encode()));
        }

        public void SendWhileCommit(long roleId, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => Send(roleId, p));
        }

        public void SendWhileCommit(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => Send(roleIds, p));
        }

        public void SendWhileRollback(long roleId, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => Send(roleId, p));
        }

        public void SendWhileRollback(ICollection<long> roleIds, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => Send(roleIds, p));
        }

        /// <summary>
        /// Func<sender, target, result>
        /// sender: 查询发起者，结果发送给他。
        /// target: 查询目标角色。
        /// result: 返回值，int，按普通事务处理过程返回值处理。
        /// </summary>
        public ConcurrentDictionary<string, Func<long, long, Binary, Task<long>>> TransmitActions { get; } = new();

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

        public void ProcessTransmit(long sender, string actionName, IEnumerable<long> roleIds, Binary parameter)
        {
            if (TransmitActions.TryGetValue(actionName, out var handle))
            {
                foreach (var target in roleIds)
                {
                    ProviderApp.Zeze.NewProcedure(async () => await handle(sender, target, parameter), "Game.Online.Transmit:" + actionName).Execute();
                }
            }
        }

        public class RoleOnServer
        {
            public int ServerId { get; set; } = -1; // empty when not online
            public HashSet<long> Roles { get; } = new();
            public void AddAll(HashSet<long> roles)
            {
                foreach (var role in roles)
                    Roles.Add(role);
            }
        }

        public async Task<ICollection<RoleOnServer>> GroupByServer(ICollection<long> roleIds)
        {
            var groups = new Dictionary<int, RoleOnServer>();
            var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.ServerId, groupNotOnline);

            foreach (var roleId in roleIds)
            {
                var online = await _tOnline.GetAsync(roleId);
                if (null == online)
                {
                    groupNotOnline.Roles.Add(roleId);
                    continue;
                }
                var version = await _tVersion.GetOrAddAsync(roleId);
                // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
                if (false == groups.TryGetValue(version.ServerId, out var group))
                {
                    group = new RoleOnServer()
                    {
                        ServerId = version.ServerId
                    };
                    groups.Add(group.ServerId, group);
                }
                group.Roles.Add(roleId);
            }
            return groups.Values;
        }

        private static RoleOnServer Merge(RoleOnServer current, RoleOnServer m)
        {
            if (null == current)
                return m;
            current.AddAll(m.Roles);
            return current;
        }

        private async Task TransmitInProcedure(long sender, string actionName, ICollection<long> roleIds, Binary parameter)
        {
            if (ProviderApp.Zeze.Config.GlobalCacheManagerHostNameOrAddress.Length == 0)
            {
                // 没有启用cache-sync，马上触发本地任务。
                ProcessTransmit(sender, actionName, roleIds, parameter);
                return;
            }

            var groups = await GroupByServer(roleIds);
            RoleOnServer groupLocal = null;
            foreach (var group in groups)
            {
                if (group.ServerId == -1 || group.ServerId == ProviderApp.Zeze.Config.ServerId)
                {
                    // loopback 就是当前gs.
                    // 对于不在线的角色，直接在本机运行。
                    groupLocal = Merge(groupLocal, group);
                    continue;
                }

                var transmit = new Transmit();
                transmit.Argument.ActionName = actionName;
                transmit.Argument.Sender = sender;
                transmit.Argument.Roles.AddAll(group.Roles);
                if (null != parameter)
                {
                    transmit.Argument.Parameter = parameter;
                }

                if (false == ProviderApp.ProviderDirectService.ProviderByServerId.TryGetValue(group.ServerId, out var ps))
                {
                    groupLocal.AddAll(group.Roles);
                    continue;
                }
                var socket = ProviderApp.ProviderDirectService.GetSocket(ps.SessionId);
                if (null == socket)
                {
                    groupLocal.AddAll(group.Roles);
                    continue;
                }
                transmit.Send(socket);
            }
            if (groupLocal.Roles.Count > 0)
                ProcessTransmit(sender, actionName, groupLocal.Roles, parameter);
        }

        public void Transmit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);

            var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.Encode(parameter));
            // 发送协议请求在另外的事务中执行。
            _ = ProviderApp.Zeze.NewProcedure(async () =>
            {
                await TransmitInProcedure(sender, actionName, roleIds, binaryParam);
                return ResultCode.Success;
            }, "Onlines.Transmit").CallAsync();
        }

        public void TransmitWhileCommit(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileCommit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleIds, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleIds, parameter));
        }

        private void Broadcast(long typeId, Binary fullEncodedProtocol, int time)
        {
            var broadcast = new Broadcast();
            broadcast.Argument.ProtocolType = typeId;
            broadcast.Argument.ProtocolWholeData = fullEncodedProtocol;
            broadcast.Argument.Time = time;

            foreach (var link in ProviderApp.ProviderService.Links.Values)
            {
                link.Socket?.Send(broadcast);
            }

        }

        public void Broadcast(Protocol p, int time = 60 * 1000)
        {
            Broadcast(p.TypeId, new Binary(p.Encode()), time);
        }

        private void VerifyLocal(Util.SchedulerTask thisTask)
        {
            long roleId = 0;
            _tLocal.WalkCache(
                (k, v) =>
                {
                    // 先得到roleId
                    roleId = k;
                    return true;
                },
                () =>
                {
                    // 锁外执行事务
                    try
                    {
                        ProviderApp.Zeze.NewProcedure(async () =>
                        {
                            await TryRemoveLocal(roleId);
                            return 0L;
                        }, "Online.VerifyLocal").CallSynchronously();
                    }
                    catch (Exception e)
                    {
                        logger.Error(e);
                    }
                });
            // 随机开始时间，避免验证操作过于集中。3:10 - 5:10
            VerifyLocalTimer = Util.Scheduler.ScheduleAt(VerifyLocal, 3 + Util.Random.Instance.Next(3), 10); // at 3:10 - 6:10
        }

        private async Task TryRemoveLocal(long roleId)
        {
            var online = await _tOnline.GetAsync(roleId);
            var local = await _tLocal.GetAsync(roleId);
            var version = await _tVersion.GetOrAddAsync(roleId);
            if (null == local)
                return;
            // null == online && null == local -> do nothing
            // null != online && null == local -> do nothing

            if ((null == online) || (version.LoginVersion != local.LoginVersion))
                await RemoveLocalAndTrigger(roleId);
        }

        [RedirectToServer]
        protected virtual async Task RedirectNotify(int serverId, long roleId)
        {
            await TryRemoveLocal(roleId);
        }

        private async Task TryRedirectNotify(int serverId, long roleId)
        {
            if (ProviderApp.Zeze.Config.ServerId != serverId
                && ProviderApp.ProviderDirectService.ProviderByServerId.ContainsKey(serverId))
            {
                await RedirectNotify(serverId, roleId);
            }
        }

        protected override async Task<long> ProcessLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Login;
            var session = ProviderUserSession.Get(rpc);

            var online = await _tOnline.GetOrAddAsync(rpc.Argument.RoleId);
            var local = await _tLocal.GetOrAddAsync(rpc.Argument.RoleId);
            var version = await _tVersion.GetOrAddAsync(rpc.Argument.RoleId);

            if (version.LoginVersion != version.LogoutVersion)
            {
                // login exist
                version.LogoutVersion = version.LoginVersion;
                await LogoutTriggerExtra(rpc.Argument.RoleId);
                if (version.LoginVersion != local.LoginVersion)
                {
                    _ = TryRedirectNotify(version.ServerId, rpc.Argument.RoleId);
                }
            }
            var loginVersion = version.LoginVersion + 1;
            version.LoginVersion = loginVersion;
            version.LoginVersion = loginVersion;
            local.LoginVersion = loginVersion;

            if (!online.LinkName.Equals(session.LinkName) || online.LinkSid != session.LinkSid)
            {
                ProviderApp.ProviderService.Kick(online.LinkName, online.LinkSid,
                        BKick.ErrorDuplicateLogin, "duplicate role login");
            }

            /////////////////////////////////////////////////////////////
            // 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
            // 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
            // see Linkd.StableLinkSid
            if (false == online.LinkName.Equals(session.LinkName))
                online.LinkName = session.LinkName;
            if (online.LinkSid != session.LinkSid)
                online.LinkSid = session.LinkSid;
            /////////////////////////////////////////////////////////////

            version.ReliableNotifyConfirmIndex = 0;
            version.ReliableNotifyIndex = 0;
            version.ReliableNotifyMark.Clear();
            await OpenQueue(rpc.Argument.RoleId).ClearAsync();

            var linkSession = session.Link.UserState as ProviderService.LinkSession;
            version.ServerId = ProviderApp.Zeze.Config.ServerId;

            await LoginTrigger(rpc.Argument.RoleId);

            // 先提交结果再设置状态。
            // see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                setUserState.Argument.Context = rpc.Argument.RoleId.ToString();
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });
            //App.Load.LoginCount.IncrementAndGet();
            return ResultCode.Success;
        }

        protected override async Task<long> ProcessReLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReLogin;
            var session = ProviderUserSession.Get(rpc);

            BOnline online = await _tOnline.GetAsync(rpc.Argument.RoleId);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            var local = await _tLocal.GetOrAddAsync(rpc.Argument.RoleId);
            var version = await _tVersion.GetOrAddAsync(rpc.Argument.RoleId);

            if (version.LoginVersion != local.LoginVersion)
            {
                _ = TryRedirectNotify(version.ServerId, rpc.Argument.RoleId);
            }
            /*
            if (version.LoginVersion != version.LogoutVersion)
            {
                version.LogoutVersion = version.LoginVersion;
                // login exist
                // relogin 不需要补充 Logout？
                // await LogoutTriggerExtra(rpc.Argument.RoleId);
            }
            */
            var loginVersion = version.LoginVersion + 1;
            version.LoginVersion = loginVersion;
            version.LoginVersion = loginVersion;
            local.LoginVersion = loginVersion;

            /////////////////////////////////////////////////////////////
            // 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
            // 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
            // see Linkd.StableLinkSid
            if (false == online.LinkName.Equals(session.LinkName))
                online.LinkName = session.LinkName;
            if (online.LinkSid != session.LinkSid)
                online.LinkSid = session.LinkSid;
            /////////////////////////////////////////////////////////////

            await ReloginTrigger(rpc.Argument.RoleId);

            // 先发结果，再发送同步数据（ReliableNotifySync）。
            // 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                setUserState.Argument.Context = rpc.Argument.RoleId.ToString();
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });

            var syncResultCode = await ReliableNotifySync(rpc.Argument.RoleId,
                session, rpc.Argument.ReliableNotifyConfirmIndex);

            if (syncResultCode != ResultCodeSuccess)
                return ErrorCode((ushort)syncResultCode);

            //App.Load.LoginCount.IncrementAndGet();
            return ResultCode.Success;
        }

        protected override async Task<long> ProcessLogoutRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Logout;
            var session = ProviderUserSession.Get(rpc);

            if (session.RoleId == null)
                return ErrorCode(ResultCodeNotLogin);

            var local = await _tLocal.GetAsync(session.RoleId.Value);
            var online = await _tOnline.GetAsync(session.RoleId.Value);
            var version = await _tVersion.GetOrAddAsync(session.RoleId.Value);
            // 登录在其他机器上。
            if (local == null && online != null)
            {
                _ = TryRedirectNotify(version.ServerId, session.RoleId.Value); // nowait
            }
            if (null != local)
                await RemoveLocalAndTrigger(session.RoleId.Value);
            if (null != online)
            {
                version.LogoutVersion = version.LoginVersion;
                await LogoutTrigger(session.RoleId.Value);
            }

            // 先设置状态，再发送Logout结果。
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });
            session.SendResponseWhileCommit(rpc);
            // 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
            // App.Load.LogoutCount.IncrementAndGet();
            return ResultCode.Success;
        }

        private async Task<int> ReliableNotifySync(long roleId, ProviderUserSession session, long index, bool sync = true)
        {
            var online = await _tVersion.GetOrAddAsync(roleId);
            var queue = OpenQueue(roleId);
            if (index < online.ReliableNotifyConfirmIndex
                || index > online.ReliableNotifyIndex
                || index - online.ReliableNotifyConfirmIndex > await queue.CountAsync())
            {
                return ResultCodeReliableNotifyConfirmIndexOutOfRange;
            }

            int confirmCount = (int)(index - online.ReliableNotifyConfirmIndex);

            for (int i = 0; i < confirmCount; i++)
                await queue.PollAsync();

            if (sync)
            {
                var notify = new SReliableNotify();
                notify.Argument.ReliableNotifyIndex = index;
                await queue.WalkAsync((node, bNofity) =>
                {
                    notify.Argument.Notifies.Add(bNofity.FullEncodedProtocol);
                    return true;
                });
                session.SendResponseWhileCommit(notify);
            }
            online.ReliableNotifyConfirmIndex = index;
            return ResultCodeSuccess;
        }

        protected override async Task<long> ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReliableNotifyConfirm;
            var session = ProviderUserSession.Get(rpc);

            BOnline online = await _tOnline.GetAsync(session.RoleId.Value);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            session.SendResponseWhileCommit(rpc); // 同步前提交。
            var syncResultCode = await ReliableNotifySync(session.RoleId.Value,
                session, rpc.Argument.ReliableNotifyConfirmIndex, rpc.Argument.Sync);

            if (ResultCodeSuccess != syncResultCode)
                return ErrorCode((ushort)syncResultCode);

            return ResultCode.Success;
        }

    }
}
