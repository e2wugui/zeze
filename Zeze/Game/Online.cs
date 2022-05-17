
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.Game.Online;
using Zeze.Builtin.Provider;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

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

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public ProviderApp ProviderApp { get; }
        public LoadReporter LoadReporter { get; }
        public taccount TableAccount => _taccount;

        public Online(ProviderApp app)
        {
            this.ProviderApp = app;

            RegisterProtocols(ProviderApp.ProviderService);
            RegisterZezeTables(ProviderApp.Zeze);

            LoadReporter = new(this);
        }

        public override void UnRegister()
        {
            UnRegisterZezeTables(ProviderApp.Zeze);
            UnRegisterProtocols(ProviderApp.ProviderService);
        }
        public void Start()
        {
            LoadReporter.StartTimerTask();
            Util.Scheduler.ScheduleAt(VerifyLocal, 3 + Util.Random.Instance.Next(3), 10); // at 3:10 - 6:10
        }

        public int LocalCount => _tlocal.Cache.DataMap.Count;

        public long WalkLocal(Func<long, BLocal, bool> walker)
        {
            return _tlocal.WalkCache(walker);
        }

        public async Task SetLocalBean(long roleId, string key, Bean bean)
        {
            var bLocal = await _tlocal.GetAsync(roleId);
            if (null == bLocal)
                throw new Exception("roleid not online. " + roleId);
            var bAny = new BAny();
            bAny.Any.Bean = bean;
            bLocal.Datas[key] = bAny;
        }

        public async Task<T> GetLocalBean<T>(long roleId, string key)
            where T : Bean
        {
            var bLocal = await _tlocal.GetAsync(roleId);
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

        private Util.AtomicLong _LoginTimes = new();

        public long LoginTimes => _LoginTimes.Get();

        public async Task<bool> AddRole(string account, long roleId)
        {
            BAccount bAccount = await _taccount.GetOrAddAsync(account);
            if (!bAccount.Name.Equals(account)) // 优化写，相同的时候不修改数据。
                bAccount.Name = account;
            if (bAccount.Roles.Contains(roleId))
                return false;
            bAccount.Roles.Add(roleId);
            return true;
        }

        public async Task RemoveRole(String account, long roleId)
        {
            BAccount bAccount = await _taccount.GetAsync(account);
            bAccount?.Roles.Remove(roleId);
        }

        public async Task<bool> SetLastLoginRoleId(String account, long roleId)
        {
            BAccount bAccount = await _taccount.GetAsync(account);
            if (bAccount == null)
                return false;
            if (!bAccount.Roles.Contains(roleId))
                return false;
            bAccount.LastLoginRoleId = roleId;
            return true;
        }

        private async Task RemoveLocalAndTrigger(long roleId)
        {
            var arg = new LocalRemoveEventArgument()
            {
                RoleId = roleId,
                LocalData = (await _tlocal.GetAsync(roleId)).Copy(),
            };

            await _tlocal.RemoveAsync(roleId); // remove first

            await LocalRemoveEvents.TriggerEmbed(this, arg);
            await LocalRemoveEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LocalRemoveEvents.TriggerThread(this, arg));
        }

        private async Task RemoveOnlineAndTrigger(long roleId)
        {
            var arg = new LogoutEventArgument()
            {
                RoleId = roleId,
                OnlineData = (await _tonline.GetAsync(roleId)).Copy(),
            };

            await _tonline.RemoveAsync(roleId); // remove first

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

        public async Task OnLinkBroken(long roleId, BLinkBroken arg)
        {
            long currentLoginVersion = 0;
            {
                var online = await _tonline.GetAsync(roleId);
                // skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
                if (null == online || online.LinkSid != arg.LinkSid)
                    return;
                var version = await _tversion.GetOrAddAsync(roleId);

                var local = await _tlocal.GetAsync(roleId);
                if (local == null)
                    return; // 不在本机登录。

                currentLoginVersion = local.LoginVersion;
                if (version.LoginVersion != currentLoginVersion)
                    await RemoveLocalAndTrigger(roleId); // 本机数据已经过时，马上删除。
            }
            await Task.Delay(10 * 60 * 1000);

            // TryRemove
            await ProviderApp.Zeze.NewProcedure(async () =>
            {
                // local online 独立判断version分别尝试删除。
                var local = await _tlocal.GetAsync(roleId);
                if (null != local && local.LoginVersion == currentLoginVersion)
                {
                    await RemoveLocalAndTrigger(roleId);
                }
                // 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
                var online = await _tonline.GetAsync(roleId);
                var version = await _tversion.GetOrAddAsync(roleId);
                if (null != online && version.LoginVersion == currentLoginVersion)
                {
                    await RemoveOnlineAndTrigger(roleId);
                }
                return Procedure.Success;
            }, "Onlines.OnLinkBroken").CallAsync();
        }

        public async Task AddReliableNotifyMark(long roleId, string listenerName)
        {
            var online = await _tonline.GetAsync(roleId);
            if (null == online)
                throw new Exception("Not Online. AddReliableNotifyMark: " + listenerName);
            (await _tversion.GetOrAddAsync(roleId)).ReliableNotifyMark.Add(listenerName);
        }

        public async Task RemoveReliableNotifyMark(long roleId, string listenerName)
        {
            // 移除尽量通过，不做任何判断。
            (await _tversion.GetOrAddAsync(roleId)).ReliableNotifyMark.Remove(listenerName);
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
                    BOnline online = await _tonline.GetAsync(roleId);
                    if (null == online)
                    {
                        // 完全离线，忽略可靠消息发送：可靠消息仅仅为在线提供服务，并不提供全局可靠消息。
                        return Procedure.Success;
                    }
                    var version = await _tversion.GetOrAddAsync(roleId);
                    if (false == version.ReliableNotifyMark.Contains(listenerName))
                    {
                        return Procedure.Success; // 相关数据装载的时候要同步设置这个。
                    }

                    // 先保存在再发送，然后客户端还会确认。
                    // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
                    version.ReliableNotifyQueue.Add(fullEncodedProtocol);
                    version.ReliableNotifyIndex += 1;

                    var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
                    notify.Argument.ReliableNotifyIndex = version.ReliableNotifyIndex;
                    notify.Argument.Notifies.Add(fullEncodedProtocol);

                    await SendInProcedure(new List<long> { roleId }, notify.TypeId, new Binary(notify.Encode()));
                    return Procedure.Success;
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
            public Dictionary<long, long> Roles { get; } = new();
        }

        public async Task<ICollection<RoleOnLink>> GroupByLink(ICollection<long> roleIds)
        {
            var groups = new Dictionary<string, RoleOnLink>();
            var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.LinkName, groupNotOnline);

            foreach (var roleId in roleIds)
            {
                var online = await _tonline.GetAsync(roleId);
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
                        ServerId = (await _tversion.GetOrAddAsync(roleId)).ServerId,
                    };
                    groups.Add(group.LinkName, group);
                }
                group.Roles.TryAdd(roleId, online.LinkSid);
            }
            return groups.Values;
        }

        private async Task SendInProcedure(ICollection<long> roles, long typeId, Binary fullEncodedProtocol)
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
                        send.Argument.LinkSids.UnionWith(group.Roles.Values);
                        group.LinkSocket.Send(send);
                }
            });
        }

        private void Send(long roleId, long typeId, Binary fullEncodedProtocol)
        {
            // 发送协议请求在另外的事务中执行。
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(roleId, () =>
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendInProcedure(new List<long> { roleId }, typeId, fullEncodedProtocol);
                    return Procedure.Success;
                }, "Onlines.Send"));
        }

        private void Send(ICollection<long> roles, long typeId, Binary fullEncodedProtocol)
        {

            // 发送协议请求在另外的事务中执行。
            if (roles.Count > 0)
            {
                ProviderApp.Zeze.TaskOneByOneByKey.ExecuteCyclicBarrier(roles,
                    ProviderApp.Zeze.NewProcedure(async () =>
                    {
                        await SendInProcedure(roles, typeId, fullEncodedProtocol);
                        return Procedure.Success;
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
                var online = await _tonline.GetAsync(roleId);
                if (null == online)
                {
                    groupNotOnline.Roles.Add(roleId);
                    continue;
                }
                var version = await _tversion.GetOrAddAsync(roleId);
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

        private RoleOnServer Merge(RoleOnServer current, RoleOnServer m)
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
                throw new Exception("Unkown Action Name: " + actionName);

            var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.Encode(parameter));
            // 发送协议请求在另外的事务中执行。
            _ = ProviderApp.Zeze.NewProcedure(async () =>
            {
                await TransmitInProcedure(sender, actionName, roleIds, binaryParam);
                return Procedure.Success;
            }, "Onlines.Transmit").CallAsync();
        }

        public void TransmitWhileCommit(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileCommit(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(sender, actionName, roleIds, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, long roleId, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(sender, actionName, roleId, parameter));
        }

        public void TransmitWhileRollback(long sender, string actionName, ICollection<long> roleIds, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unkown Action Name: " + actionName);
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
            _tlocal.WalkCache(
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
                        }, "VerifyLocal:" + roleId).CallSynchronously();
                    }
                    catch (Exception e)
                    {
                        logger.Error(e);
                    }
                });
            // 随机开始时间，避免验证操作过于集中。3:10 - 5:10
            Util.Scheduler.ScheduleAt(VerifyLocal, 3 + Util.Random.Instance.Next(3), 10); // at 3:10 - 6:10
        }

        private async Task TryRemoveLocal(long roleId)
        {
            var online = await _tonline.GetAsync(roleId);
            var local = await _tlocal.GetAsync(roleId);
            var version = await _tversion.GetOrAddAsync(roleId);
            if (null == local)
                return;
            // null == online && null == local -> do nothing
            // null != online && null == local -> do nothing

            if ((null == online) || (version.LoginVersion != local.LoginVersion))
                await RemoveLocalAndTrigger(roleId);
        }

        [RedirectToServer]
        protected async Task RedirectNotify(int serverId, long roleId)
        {
            await TryRemoveLocal(roleId);
        }

        protected override async Task<long> ProcessLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Login;
            var session = ProviderUserSession.Get(rpc);

            var account = await _taccount.GetOrAddAsync(session.Account);
            if (!account.Roles.Contains(rpc.Argument.RoleId))
                return ErrorCode(ResultCodeRoleNotExist);

            account.LastLoginRoleId = rpc.Argument.RoleId;

            var online = await _tonline.GetOrAddAsync(rpc.Argument.RoleId);
            var local = await _tlocal.GetOrAddAsync(rpc.Argument.RoleId);
            var version = await _tversion.GetOrAddAsync(rpc.Argument.RoleId);

            // login exist && not local
            if (version.LoginVersion != 0 && version.LoginVersion != local.LoginVersion)
            {
                // nowait
                _ = RedirectNotify(version.ServerId, rpc.Argument.RoleId);
            }
            var loginVersion = account.LastLoginVersion + 1;
            account.LastLoginVersion = loginVersion;
            version.LoginVersion = loginVersion;
            local.LoginVersion = loginVersion;

            if (!online.LinkName.Equals(session.LinkName) || online.LinkSid == session.LinkSid)
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
            version.ReliableNotifyQueue.Clear();

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
            return Procedure.Success;
        }

        protected override async Task<long> ProcessReLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReLogin;
            var session = ProviderUserSession.Get(rpc);

            BAccount account = await _taccount.GetAsync(session.Account);
            if (null == account)
                return ErrorCode(ResultCodeAccountNotExist);

            if (account.LastLoginRoleId != rpc.Argument.RoleId)
                return ErrorCode(ResultCodeNotLastLoginRoleId);

            if (!account.Roles.Contains(rpc.Argument.RoleId))
                return ErrorCode(ResultCodeRoleNotExist);

            BOnline online = await _tonline.GetAsync(rpc.Argument.RoleId);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            var local = await _tlocal.GetOrAddAsync(rpc.Argument.RoleId);
            var version = await _tversion.GetOrAddAsync(rpc.Argument.RoleId);

            // login exist && not local
            if (version.LoginVersion != 0 && version.LoginVersion != local.LoginVersion)
            {
                // nowait
                _ = RedirectNotify(version.ServerId, rpc.Argument.RoleId);
            }
            var loginVersion = account.LastLoginVersion + 1;
            account.LastLoginVersion = loginVersion;
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

            await ReloginTrigger(session.RoleId.Value);

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

            var syncResultCode = await ReliableNotifySync(session.RoleId.Value,
                session, rpc.Argument.ReliableNotifyConfirmIndex);

            if (syncResultCode != ResultCodeSuccess)
                return ErrorCode((ushort)syncResultCode);

            //App.Load.LoginCount.IncrementAndGet();
            return Procedure.Success;
        }

        protected override async Task<long> ProcessLogoutRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Logout;
            var session = ProviderUserSession.Get(rpc);

            if (session.RoleId == null)
                return ErrorCode(ResultCodeNotLogin);

            var local = await _tlocal.GetAsync(session.RoleId.Value);
            var online = await _tonline.GetAsync(session.RoleId.Value);
            var version = await _tversion.GetOrAddAsync(session.RoleId.Value);
            // 登录在其他机器上。
            if (local == null && online != null)
                _ = RedirectNotify(version.ServerId, session.RoleId.Value); // nowait
            if (null != local)
                await RemoveLocalAndTrigger(session.RoleId.Value);
            if (null != online)
                await RemoveOnlineAndTrigger(session.RoleId.Value);

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
            return Procedure.Success;
        }

        private async Task<int> ReliableNotifySync(long roleId, ProviderUserSession session, long index, bool sync = true)
        {
            var online = await _tversion.GetOrAddAsync(roleId);
            if (index < online.ReliableNotifyConfirmIndex
                || index > online.ReliableNotifyIndex
                || index - online.ReliableNotifyConfirmIndex > online.ReliableNotifyQueue.Count)
            {
                return ResultCodeReliableNotifyConfirmIndexOutOfRange;
            }

            int confirmCount = (int)(index - online.ReliableNotifyConfirmIndex);

            if (sync)
            {
                var notify = new SReliableNotify();
                notify.Argument.ReliableNotifyIndex = index;
                for (int i = confirmCount; i < online.ReliableNotifyQueue.Count; ++i)
                    notify.Argument.Notifies.Add(online.ReliableNotifyQueue[i]);
                session.SendResponseWhileCommit(notify);
            }
            online.ReliableNotifyQueue.RemoveRange(0, confirmCount);
            online.ReliableNotifyConfirmIndex = index;
            return ResultCodeSuccess;
        }

        protected override async Task<long> ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReliableNotifyConfirm;
            var session = ProviderUserSession.Get(rpc);

            BOnline online = await _tonline.GetAsync(session.RoleId.Value);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            session.SendResponseWhileCommit(rpc); // 同步前提交。
            var syncResultCode = await ReliableNotifySync(session.RoleId.Value,
                session, rpc.Argument.ReliableNotifyConfirmIndex, rpc.Argument.Sync);

            if (ResultCodeSuccess != syncResultCode)
                return ErrorCode((ushort)syncResultCode);

            return Procedure.Success;
        }

    }
}
