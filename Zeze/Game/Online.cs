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
                // skip not owner: �������LinkSid�ǲ���ֵġ�����������LoginVersion��
                if (null == online || false == online.LinkName.Equals(linkName) || online.LinkSid != linkSid)
                    return;
                var version = await _tVersion.GetOrAddAsync(roleId);

                var local = await _tLocal.GetAsync(roleId);
                if (local == null)
                    return; // ���ڱ�����¼��

                currentLoginVersion = local.LoginVersion;
                if (version.LoginVersion != currentLoginVersion)
                    await RemoveLocalAndTrigger(roleId); // ���������Ѿ���ʱ������ɾ����
            }
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                // TODO ʵ��Timer�Ժ�����ĳ�ʹ�������������ĳ־û����Դﵽ����ʧLogout��Ŀ�ġ�
                Util.Scheduler.Schedule(async (ThisTask) =>
                {
                    // TryRemove
                    await ProviderApp.Zeze.NewProcedure(async () =>
                    {
                    // local online �����ж�version�ֱ���ɾ����
                    var local = await _tLocal.GetAsync(roleId);
                        if (null != local && local.LoginVersion == currentLoginVersion)
                        {
                            await RemoveLocalAndTrigger(roleId);
                        }
                    // ���������ӳ��ڼ佨�����µĵ�¼������汾���жϻ�ʧ�ܡ�
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
            // �Ƴ�����ͨ���������κ��жϡ�
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
        /// �������߿ɿ�Э�飬��������ߵȣ���Ȼ���ᷢ��Ŷ��
        /// </summary>
        /// <param name="roleId"></param>
        /// <param name="listenerName"></param>
        /// <param name="fullEncodedProtocol">Э������ȱ��룬��Ϊ�������</param>
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
                        // ��ȫ���ߣ����Կɿ���Ϣ���ͣ��ɿ���Ϣ����Ϊ�����ṩ���񣬲����ṩȫ�ֿɿ���Ϣ��
                        return ResultCode.Success;
                    }
                    var version = await _tVersion.GetOrAddAsync(roleId);
                    if (false == version.ReliableNotifyMark.Contains(listenerName))
                    {
                        return ResultCode.Success; // �������װ�ص�ʱ��Ҫͬ�����������
                    }

                    // �ȱ������ٷ��ͣ�Ȼ��ͻ��˻���ȷ�ϡ�
                    // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm ��ʵ�֡�
                    var queue = OpenQueue(roleId);
                    await queue.AddAsync(new BNotify() { FullEncodedProtocol = fullEncodedProtocol });

                    var notify = new SReliableNotify(); // ��ֱ�ӷ���Э�飬����Ϊ�ͻ�����Ҫʶ��ReliableNotify�����д�����������
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
                // ���汣��connector.Socket��ʹ�ã����֮�����ӱ��رգ��Ժ���Э��ʧ�ܡ�
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
            // ������ϢΪ������TaskOneByOne��ֻ��һ��һ�����ͣ�Ϊ���ٸĴ��룬��ʹ�þɵ�GroupByLink�ӿڡ�
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
            // ����Э�������������������ִ�С�
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(roleId, () =>
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendEmbed(new List<long> { roleId }, typeId, fullEncodedProtocol);
                    return ResultCode.Success;
                }, "Onlines.Send"));
        }

        public void Send(ICollection<long> roles, long typeId, Binary fullEncodedProtocol)
        {

            // ����Э�������������������ִ�С�
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
        /// sender: ��ѯ�����ߣ�������͸�����
        /// target: ��ѯĿ���ɫ��
        /// result: ����ֵ��int������ͨ��������̷���ֵ����
        /// </summary>
        public ConcurrentDictionary<string, Func<long, long, Binary, Task<long>>> TransmitActions { get; } = new();

        /// <summary>
        /// ת����ѯ�����RoleId��
        /// </summary>
        /// <param name="sender">��ѯ�����ߣ�������͸�����</param>
        /// <param name="actionName">��ѯ�����ʵ��</param>
        /// <param name="roleId">Ŀ���ɫ</param>
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
                // ���汣��connector.Socket��ʹ�ã����֮�����ӱ��رգ��Ժ���Э��ʧ�ܡ�
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
                // û������cache-sync�����ϴ�����������
                ProcessTransmit(sender, actionName, roleIds, parameter);
                return;
            }

            var groups = await GroupByServer(roleIds);
            RoleOnServer groupLocal = null;
            foreach (var group in groups)
            {
                if (group.ServerId == -1 || group.ServerId == ProviderApp.Zeze.Config.ServerId)
                {
                    // loopback ���ǵ�ǰgs.
                    // ���ڲ����ߵĽ�ɫ��ֱ���ڱ������С�
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
            // ����Э�������������������ִ�С�
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
                    // �ȵõ�roleId
                    roleId = k;
                    return true;
                },
                () =>
                {
                    // ����ִ������
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
            // �����ʼʱ�䣬������֤�������ڼ��С�3:10 - 5:10
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
            // ��LinkName,LinkSidû�б仯��ʱ�򣬱��ּ�¼�Ƕ�ȡ״̬����������д����
            // ��ΪOnline���ݿ��ܻᱻ�ܶ�ط����棬д��������ɻ���ʧЧ��
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

            // ���ύ���������״̬��
            // see linkd::Zezex.Provider.ModuleProvider��ProcessBroadcast
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                setUserState.Argument.Context = rpc.Argument.RoleId.ToString();
                rpc.Sender.Send(setUserState); // ֱ��ʹ��link���ӡ�
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
                // relogin ����Ҫ���� Logout��
                // await LogoutTriggerExtra(rpc.Argument.RoleId);
            }
            */
            var loginVersion = version.LoginVersion + 1;
            version.LoginVersion = loginVersion;
            version.LoginVersion = loginVersion;
            local.LoginVersion = loginVersion;

            /////////////////////////////////////////////////////////////
            // ��LinkName,LinkSidû�б仯��ʱ�򣬱��ּ�¼�Ƕ�ȡ״̬����������д����
            // ��ΪOnline���ݿ��ܻᱻ�ܶ�ط����棬д��������ɻ���ʧЧ��
            // see Linkd.StableLinkSid
            if (false == online.LinkName.Equals(session.LinkName))
                online.LinkName = session.LinkName;
            if (online.LinkSid != session.LinkSid)
                online.LinkSid = session.LinkSid;
            /////////////////////////////////////////////////////////////

            await ReloginTrigger(rpc.Argument.RoleId);

            // �ȷ�������ٷ���ͬ�����ݣ�ReliableNotifySync����
            // ��ʹ�� WhileCommit������ɹ������ύ��˳���ͣ�ʧ��ȫ�����ᷢ�͡�
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                setUserState.Argument.Context = rpc.Argument.RoleId.ToString();
                rpc.Sender.Send(setUserState); // ֱ��ʹ��link���ӡ�
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
            // ��¼�����������ϡ�
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

            // ������״̬���ٷ���Logout�����
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                rpc.Sender.Send(setUserState); // ֱ��ʹ��link���ӡ�
            });
            session.SendResponseWhileCommit(rpc);
            // �� OnLinkBroken ʱ��������ͬʱ���������쳣�������
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

            session.SendResponseWhileCommit(rpc); // ͬ��ǰ�ύ��
            var syncResultCode = await ReliableNotifySync(session.RoleId.Value,
                session, rpc.Argument.ReliableNotifyConfirmIndex, rpc.Argument.Sync);

            if (ResultCodeSuccess != syncResultCode)
                return ErrorCode((ushort)syncResultCode);

            return ResultCode.Success;
        }

    }
}
