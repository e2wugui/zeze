using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Arch.Gen;
using Zeze.Builtin.Online;
using Zeze.Builtin.Provider;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Arch
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
        public ProviderLoad Load { get; }

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
            this.App = app ?? throw new ArgumentException("app is null");
            this.ProviderApp = app.Zeze.Redirect.ProviderApp;

            Load = new(this);
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
            Load.Start();
            VerifyLocalTimer = Util.Scheduler.ScheduleAt(VerifyLocal, 3 + Util.Random.Instance.Next(3), 10); // at 3:10 - 6:10
            ProviderApp.BuiltinModules.Add(FullName, this);
        }

        public void Stop()
        {
            Load.Stop();
            VerifyLocalTimer?.Cancel();
        }

        public int LocalCount => _tLocal.Cache.DataMap.Count;

        public long WalkLocal(Func<string, BLocals, bool> walker)
        {
            return _tLocal.WalkCache(walker);
        }

        public async Task SetLocalBean(string account, string clientId, string key, Bean bean)
        {
            var bLocals = await _tLocal.GetAsync(account);
            if (null == bLocals)
                throw new Exception("roleid not online. " + account);
            if (false == bLocals.Logins.TryGetValue(clientId, out var login))
            {
                login = new BLocal();
                bLocals.Logins.Add(clientId, login);
            }
            var bAny = new BAny();
            bAny.Any.Bean = bean;
            login.Datas[key] = bAny;
        }

        public async Task<T> GetLocalBean<T>(string account, string clientId, string key)
            where T : Bean
        {
            var bLocals = await _tLocal.GetAsync(account);
            if (null == bLocals)
                return null;
            if (!bLocals.Logins.TryGetValue(clientId, out var login))
                return null;
            if (!login.Datas.TryGetValue(key, out var data))
                return null;
            return (T)data.Any.Bean;

        }

        public Zeze.Util.EventDispatcher LoginEvents { get; } = new("Online.Login");
        public Zeze.Util.EventDispatcher ReloginEvents { get; } = new("Online.Relogin");
        public Zeze.Util.EventDispatcher LogoutEvents { get; } = new("Online.Logout");
        public Zeze.Util.EventDispatcher LocalRemoveEvents { get; } = new("Online.Local.Remove");

        private readonly Util.AtomicLong _LoginTimes = new();

        public long LoginTimes => _LoginTimes.Get();

        private async Task RemoveLocalAndTrigger(string account, string clientId)
        {
            var bLocals = await _tLocal.GetAsync(account);
            bLocals.Logins.Remove(clientId, out var localData);
            var arg = new LocalRemoveEventArgument()
            {
                Account = account,
                ClientId = clientId,
                LocalData = localData?.Copy(),
            };

            if (bLocals.Logins.Count == 0)
                await _tLocal.RemoveAsync(account); // remove first

            await LocalRemoveEvents.TriggerEmbed(this, arg);
            await LocalRemoveEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LocalRemoveEvents.TriggerThread(this, arg));
        }

        private async Task LogoutTriggerExtra(string account, string clientId)
        {
            var bOnlines = await _tOnline.GetAsync(account);
            bOnlines.Logins.TryGetValue(clientId, out var onlineData);

            var arg = new LogoutEventArgument()
            {
                Account = account,
                ClientId = clientId,
                OnlineData = onlineData?.Copy(),
            };

            await LogoutEvents.TriggerEmbed(this, arg);
            await LogoutEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LogoutEvents.TriggerThread(this, arg));
        }

        private async Task LogoutTrigger(string account, string clientId)
        {
            var bOnlines = await _tOnline.GetAsync(account);
            bOnlines.Logins.Remove(clientId, out var onlineData);

            var arg = new LogoutEventArgument()
            {
                Account = account,
                ClientId = clientId,
                OnlineData = onlineData?.Copy(),
            };

            if (bOnlines.Logins.Count == 0)
                await _tOnline.RemoveAsync(account); // remove first

            await LogoutEvents.TriggerEmbed(this, arg);
            await LogoutEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LogoutEvents.TriggerThread(this, arg));
        }

        private async Task LoginTrigger(string account, string clientId)
        {
            var arg = new LoginArgument()
            {
                Account = account,
                ClientId = clientId,
            };

            await LoginEvents.TriggerEmbed(this, arg);
            await LoginEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => LoginEvents.TriggerThread(this, arg));
            _LoginTimes.IncrementAndGet();
        }

        private async Task ReloginTrigger(string account, string clientId)
        {
            var arg = new LoginArgument()
            {
                Account = account,
                ClientId = clientId,
            };

            await ReloginEvents.TriggerEmbed(this, arg);
            await ReloginEvents.TriggerProcedure(ProviderApp.Zeze, this, arg);
            Transaction.Transaction.Current.RunWhileCommit(() => ReloginEvents.TriggerThread(this, arg));
            _LoginTimes.IncrementAndGet();
        }

        public async Task OnLinkBroken(string account, string clientId, long linkSid, string linkName)
        {
            long currentLoginVersion = 0;
            {
                var online = await _tOnline.GetAsync(account);
                if (false == online.Logins.TryGetValue(clientId, out var loginOnline))
                    return;
                // skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
                if (false == loginOnline.LinkName.Equals(linkName) || loginOnline.LinkSid != linkSid)
                    return;

                var version = await _tVersion.GetOrAddAsync(account);
                var local = await _tLocal.GetAsync(account);
                if (local == null || false == local.Logins.TryGetValue(clientId, out var loginLocal))
                    return; // 不在本机登录。
                if (false == version.Logins.TryGetValue(clientId, out var loginVersion))
                    return; // 不存在登录。
                currentLoginVersion = loginLocal.LoginVersion;
                if (loginVersion.LoginVersion != currentLoginVersion)
                    await RemoveLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
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
                        var local = await _tLocal.GetAsync(account);
                        if (null != local
                            && local.Logins.TryGetValue(clientId, out var loginLocal)
                            && loginLocal.LoginVersion == currentLoginVersion)
                        {
                            await RemoveLocalAndTrigger(account, clientId);
                        }
                        // 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
                        var online = await _tOnline.GetAsync(account);
                        var version = await _tVersion.GetOrAddAsync(account);
                        if (null != online
                            && version.Logins.TryGetValue(clientId, out var loginVersion)
                            && loginVersion.LoginVersion == currentLoginVersion)
                        {
                            loginVersion.LogoutVersion = loginVersion.LoginVersion;
                            await LogoutTrigger(account, clientId);
                        }
                        return ResultCode.Success;
                    }, "Onlines.OnLinkBroken").CallAsync();
                }, ProviderApp.Zeze.Config.OnlineLogoutDelay);
            });
        }

        public async Task AddReliableNotifyMark(string account, string clientId, string listenerName)
        {
            var online = await _tOnline.GetAsync(account);
            if (null == online)
                throw new Exception("Not Online. AddReliableNotifyMark: " + listenerName);
            var version = await _tVersion.GetOrAddAsync(account);
            version.Logins.GetOrAdd(clientId).ReliableNotifyMark.Add(listenerName);
        }

        public async Task RemoveReliableNotifyMark(string account, string clientId, string listenerName)
        {
            // 移除尽量通过，不做任何判断。
            if ((await _tVersion.GetOrAddAsync(account)).Logins.TryGetValue(clientId, out var login))
                login.ReliableNotifyMark.Remove(listenerName);
        }

        public void SendReliableNotifyWhileCommit(
            string account, string clientId, string listenerName, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(account, clientId, listenerName, p)
                );
        }

        public void SendReliableNotifyWhileCommit(
            string account, string clientId, string listenerName, int typeId, Binary fullEncodedProtocol)
        {
            Transaction.Transaction.Current.RunWhileCommit(
                () => SendReliableNotify(account, clientId, listenerName, typeId, fullEncodedProtocol)
                );
        }

        public void SendReliableNotifyWhileRollback(string account, string clientId, string listenerName, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(account, clientId, listenerName, p)
                );
        }

        public void SendReliableNotifyWhileRollback(
            string account, string clientId, string listenerName, int typeId, Binary fullEncodedProtocol)
        {
            Transaction.Transaction.Current.RunWhileRollback(
                () => SendReliableNotify(account, clientId, listenerName, typeId, fullEncodedProtocol)
                );
        }

        public void SendReliableNotify(string account, string clientId, string listenerName, Protocol p)
        {
            SendReliableNotify(account, clientId, listenerName, p.TypeId, new Binary(p.Encode()));
        }

        private Zeze.Collections.Queue<BNotify> OpenQueue(string account, string clientId)
        {
            return ProviderApp.Zeze.Queues.Open<BNotify>("Zeze.Arch.Online.ReliableNotifyQueue:" + account + ":" + clientId);
        }
        
        /// <summary>
        /// 发送在线可靠协议，如果不在线等，仍然不会发送哦。
        /// </summary>
        /// <param name="roleId"></param>
        /// <param name="listenerName"></param>
        /// <param name="fullEncodedProtocol">协议必须先编码，因为会跨事务。</param>
        public void SendReliableNotify(
            string account, string clientId, string listenerName, long typeId, Binary fullEncodedProtocol)
        {
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(
                listenerName,
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    var online = await _tOnline.GetAsync(account);
                    if (null == online)
                    {
                        // 完全离线，忽略可靠消息发送：可靠消息仅仅为在线提供服务，并不提供全局可靠消息。
                        return ResultCode.Success;
                    }
                    var version = await _tVersion.GetOrAddAsync(account);
                    if (false == version.Logins.TryGetValue(clientId, out var login)
                        || false == login.ReliableNotifyMark.Contains(listenerName))
                    {
                        return ResultCode.Success; // 相关数据装载的时候要同步设置这个。
                    }

                    // 先保存在再发送，然后客户端还会确认。
                    // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
                    var queue = OpenQueue(account, clientId);
                    await queue.AddAsync(new BNotify() { FullEncodedProtocol = fullEncodedProtocol });

                    var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
                    notify.Argument.ReliableNotifyIndex = login.ReliableNotifyIndex;
                    login.ReliableNotifyIndex += 1; // after set notify.Argument
                    notify.Argument.Notifies.Add(fullEncodedProtocol);

                    await SendEmbed(new List<LoginKey>{new LoginKey(account, clientId) }, notify.TypeId, new Binary(notify.Encode()));
                    return ResultCode.Success;
                },
                "SendReliableNotify." + listenerName
                ));
        }

        public class LoginOnLink
        {
            public string LinkName { get; set; } = ""; // empty when not online
            public AsyncSocket LinkSocket { get; set; } // null if not online
            public int ServerId { get; set; } = -1;
            public long ProviderSessionId { get; set; }
            public Dictionary<LoginKey, long> Logins { get; } = new();
            public Dictionary<long, (string, string)> Contexts { get; } = new(); // linkSid -> (account, clientId)
        }

        public class LoginKey
        {
            public string Account { get; set; }
            public string ClientId { get; set; }

            public LoginKey(string account, string clientId)
            {
                Account = account;
                ClientId = clientId;
            }

            public override int GetHashCode()
            {
                const int _prime_ = 31;
                int _h_ = 0;
                _h_ = _h_ * _prime_ + Account.GetHashCode();
                _h_ = _h_ * _prime_ + ClientId.GetHashCode();
                return _h_;
            }

            public override bool Equals(object obj)
            {
                if (obj == this)
                    return true;
                if (obj is LoginKey login)
                {
                    return Account.Equals(login.Account) && ClientId.Equals(login.ClientId);
                }
                return false;
            }
        }

        public async Task<ICollection<LoginOnLink>> GroupByLink(ICollection<LoginKey> logins)
        {
            var groups = new Dictionary<string, LoginOnLink>();
            var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.LinkName, groupNotOnline);

            foreach (var alogin in logins)
            {
                var online = await _tOnline.GetAsync(alogin.Account);
                if (null == online)
                {
                    groupNotOnline.Logins.TryAdd(alogin, 0);
                    continue;
                }
                if (false == online.Logins.TryGetValue(alogin.ClientId, out var login))
                {
                    groupNotOnline.Logins.TryAdd(alogin, 0);
                    continue;
                }
                if (false == ProviderApp.ProviderService.Links.TryGetValue(login.LinkName, out var connector))
                {
                    groupNotOnline.Logins.TryAdd(alogin, 0);
                    continue;
                }

                if (false == connector.IsHandshakeDone)
                {
                    groupNotOnline.Logins.TryAdd(alogin, 0);
                    continue;
                }
                // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
                if (false == groups.TryGetValue(login.LinkName, out var group))
                {
                    group = new LoginOnLink()
                    {
                        LinkName = login.LinkName,
                        LinkSocket = connector.Socket,
                        // 上面online存在Login的时，下面version也肯定存在相应的Login。
                        ServerId = (await _tVersion.GetOrAddAsync(alogin.Account)).Logins.GetOrAdd(alogin.ClientId).ServerId,
                    };
                    groups.Add(group.LinkName, group);
                }
                group.Logins.TryAdd(alogin, login.LinkSid);
                group.Contexts.TryAdd(login.LinkSid, (alogin.Account, alogin.ClientId)); // 忽略Try错误，或者更严格点，使用Add？
            }
            return groups.Values;
        }

        private async Task<long> TriggerLinkBroken(string linkName, ICollection<long> errorSids, Dictionary<long, (string, string)> context)
        {
            foreach (var sid in errorSids)
            {
                if (context.TryGetValue(sid, out var ctx))
                    await OnLinkBroken(ctx.Item1, ctx.Item2, sid, linkName);
            }
            return 0;
        }

        public void Send(AsyncSocket to, Dictionary<long, (string, string)> context, Send send)
        {
            send.Send(to, async (rpc) => await TriggerLinkBroken(
                ProviderService.GetLinkName(to),
                send.IsTimeout ? send.Argument.LinkSids : send.Result.ErrorLinkSids,
                context));
        }

        public async Task SendEmbed(ICollection<LoginKey> logins, long typeId, Binary fullEncodedProtocol)
        {
            var groups = await GroupByLink(logins);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                foreach (var group in groups)
                {
                    if (group.LinkSocket == null)
                        continue; // skip not online

                    var send = new Send();
                    send.Argument.ProtocolType = typeId;
                    send.Argument.ProtocolWholeData = fullEncodedProtocol;
                    foreach (var linkSid in group.Logins.Values)
                        send.Argument.LinkSids.Add(linkSid);
                    Send(group.LinkSocket, group.Contexts, send);
                }
            });
        }

        public void Send(string account, string clientId, long typeId, Binary fullEncodedProtocol)
        {
            var login = new LoginKey(account, clientId);
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(login,
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendEmbed(new List<LoginKey> { login }, typeId, fullEncodedProtocol);
                    return ResultCode.Success;
                }, "Onlines.Send"));
        }

        public void Send(ICollection<LoginKey> logins, long typeId, Binary fullEncodedProtocol)
        {
            ProviderApp.Zeze.TaskOneByOneByKey.ExecuteCyclicBarrier(logins,
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendEmbed(logins, typeId, fullEncodedProtocol);
                    return ResultCode.Success;
                }, "Onlines.Send"));
        }

        public void Send(string account, string clientId, Protocol p)
        {
            Send(account, clientId, p.TypeId, new Binary(p.Encode()));
        }

        public void Send(ICollection<LoginKey> logins, Protocol p)
        {
            Send(logins, p.TypeId, new Binary(p.Encode()));
        }

        public void SendWhileCommit(string account, string clientId, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => Send(account, clientId, p));
        }

        public void SendWhileCommit(ICollection<LoginKey> logins, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => Send(logins, p));
        }

        public void SendWhileRollback(string account, string clientId, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => Send(account, clientId, p));
        }

        public void SendWhileRollback(ICollection<LoginKey> logins, Protocol p)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => Send(logins, p));
        }

        public async Task<ICollection<LoginOnLink>> GroupAccountsByLink(ICollection<string> accounts)
        {
            var groups = new Dictionary<string, LoginOnLink>();
            var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.LinkName, groupNotOnline);

            foreach (var account in accounts)
            {
                var online = await _tOnline.GetAsync(account);
                if (null == online)
                {
                    groupNotOnline.Logins.TryAdd(new LoginKey(account, ""), 0);
                    continue;
                }
                foreach (var e in online.Logins)
                {
                    var alogin = new LoginKey(account, e.Key);
                    if (false == ProviderApp.ProviderService.Links.TryGetValue(e.Value.LinkName, out var connector))
                    {
                        groupNotOnline.Logins.TryAdd(alogin, 0);
                        continue;
                    }

                    if (false == connector.IsHandshakeDone)
                    {
                        groupNotOnline.Logins.TryAdd(alogin, 0);
                        continue;
                    }
                    // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
                    if (false == groups.TryGetValue(e.Value.LinkName, out var group))
                    {
                        group = new LoginOnLink()
                        {
                            LinkName = e.Value.LinkName,
                            LinkSocket = connector.Socket,
                            // 上面online存在Login的时，下面version也肯定存在相应的Login。
                            ServerId = (await _tVersion.GetOrAddAsync(alogin.Account)).Logins.GetOrAdd(alogin.ClientId).ServerId,
                        };
                        groups.Add(group.LinkName, group);
                    }
                    group.Logins.TryAdd(alogin, e.Value.LinkSid);
                }
            }
            return groups.Values;
        }

        public async Task SendAccountsEmbed(ICollection<string> accounts, long typeId, Binary fullEncodedProtocol, Func<LoginOnLink, bool> sender)
        {
            var groups = await GroupAccountsByLink(accounts);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                if (null == sender)
                {
                    foreach (var group in groups)
                    {
                        if (group.LinkSocket == null)
                            continue; // skip not online

                        var send = new Send();
                        send.Argument.ProtocolType = typeId;
                        send.Argument.ProtocolWholeData = fullEncodedProtocol;
                        foreach (var linkSid in group.Logins.Values)
                            send.Argument.LinkSids.Add(linkSid);
                        Send(group.LinkSocket, group.Contexts, send);
                    }
                }
                else
                {
                    foreach (var group in groups)
                    {
                        if (false == sender(group))
                            break;
                    }
                }
            });
        }

        /// <summary>
        /// 给账号所有的登录终端发送消息。
        /// </summary>
        /// <param name="account"></param>
        /// <param name="p"></param>
        public void SendAccount(string account, Protocol p, Func<LoginOnLink, bool> sender)
        {
            SendAccount(account, p.TypeId, new Binary(p.Encode()), sender);
        }

        public void SendAccount(string account, long typeId, Binary fullEncodedProtocol, Func<LoginOnLink, bool> sender)
        {
            ProviderApp.Zeze.TaskOneByOneByKey.Execute(account,
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendAccountsEmbed(new List<string> { account }, typeId, fullEncodedProtocol, sender);
                    return ResultCode.Success;
                }, "Onlines.Send"));
        }

        /// <summary>
        /// 给账号所有的登录终端发送消息。
        /// </summary>
        /// <param name="accounts"></param>
        /// <param name="p"></param>
        public void SendAccounts(ICollection<string> accounts, Protocol p, Func<LoginOnLink, bool> sender)
        {
            SendAccounts(accounts, p.TypeId, new Binary(p.Encode()), sender);
        }

        public void SendAccounts(ICollection<string> accounts, long typeId, Binary fullEncodedProtocol, Func<LoginOnLink, bool> sender)
        {
            ProviderApp.Zeze.TaskOneByOneByKey.ExecuteCyclicBarrier(accounts,
                ProviderApp.Zeze.NewProcedure(async () =>
                {
                    await SendAccountsEmbed(accounts, typeId, fullEncodedProtocol, sender);
                    return ResultCode.Success;
                }, "Onlines.Send"));
        }

        public void SendAccountWhileCommit(string account, Protocol p, Func<LoginOnLink, bool> sender)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => SendAccount(account, p, sender));
        }

        public void SendAccountsWhileCommit(ICollection<string> accounts, Protocol p, Func<LoginOnLink, bool> sender)
        {
            Transaction.Transaction.Current.RunWhileCommit(() => SendAccounts(accounts, p, sender));
        }

        public void SendAccountWhileRollback(string account, Protocol p, Func<LoginOnLink, bool> sender)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => SendAccount(account, p, sender));
        }

        public void SendAccountsWhileRollback(ICollection<string> accounts, Protocol p, Func<LoginOnLink, bool> sender)
        {
            Transaction.Transaction.Current.RunWhileRollback(() => SendAccounts(accounts, p, sender));
        }

        /// <summary>
        /// Func<senderAccount, senderClientId, target, result>
        /// sender: 查询发起者，结果发送给他。
        /// target: 查询目标。
        /// result: 返回值，int，按普通事务处理过程返回值处理。
        /// </summary>
        public ConcurrentDictionary<string, Func<string, string, string, Binary, Task<long>>> TransmitActions { get; } = new();

        /// <summary>
        /// 转发查询请求给RoleId。
        /// </summary>
        /// <param name="sender">查询发起者，结果发送给他。</param>
        /// <param name="actionName">查询处理的实现</param>
        /// <param name="roleId">目标角色</param>
        public void Transmit(string account, string clientId, string actionName, string target, Serializable parameter = null)
        {
            Transmit(account, clientId, actionName, new List<string> { target }, parameter);
        }

        public void ProcessTransmit(string account, string clientId, string actionName, IEnumerable<string> accounts, Binary parameter)
        {
            if (TransmitActions.TryGetValue(actionName, out var handle))
            {
                foreach (var target in accounts)
                {
                    ProviderApp.Zeze.NewProcedure(async () => await handle(account, clientId, target, parameter), "Arch.Online.Transmit:" + actionName).Execute();
                }
            }
        }

        public class RoleOnServer
        {
            public int ServerId { get; set; } = -1; // empty when not online
            public HashSet<string> Accounts { get; } = new();
            public void AddAll(HashSet<string> accounts)
            {
                foreach (var account in accounts)
                    Accounts.Add(account);
            }
        }

        public async Task<ICollection<RoleOnServer>> GroupByServer(ICollection<string> accounts)
        {
            var groups = new Dictionary<int, RoleOnServer>();
            var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
            groups.Add(groupNotOnline.ServerId, groupNotOnline);

            foreach (var account in accounts)
            {
                var online = await _tOnline.GetAsync(account);
                if (null == online)
                {
                    groupNotOnline.Accounts.Add(account);
                    continue;
                }
                var version = await _tVersion.GetOrAddAsync(account);
                if (version.Logins.Count == 0)
                {
                    // null != online 意味着这里肯定不为0，不会到达这个分支。
                    // 下面要求Logins.Count必须大于0，判断一下吧。
                    groupNotOnline.Accounts.Add(account);
                    continue;
                }
                var serverId = version.Logins.GetEnumerator().Current.Value.ServerId;
                // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
                if (false == groups.TryGetValue(serverId, out var group))
                {
                    group = new RoleOnServer()
                    {
                        ServerId = serverId
                    };
                    groups.Add(group.ServerId, group);
                }
                group.Accounts.Add(account);
            }
            return groups.Values;
        }

        private static RoleOnServer Merge(RoleOnServer current, RoleOnServer m)
        {
            if (null == current)
                return m;
            current.AddAll(m.Accounts);
            return current;
        }

        private async Task TransmitInProcedure(string account, string clientId, string actionName, ICollection<string> accounts, Binary parameter)
        {
            if (ProviderApp.Zeze.Config.GlobalCacheManagerHostNameOrAddress.Length == 0)
            {
                // 没有启用cache-sync，马上触发本地任务。
                ProcessTransmit(account, clientId, actionName, accounts, parameter);
                return;
            }

            var groups = await GroupByServer(accounts);
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

                var transmit = new TransmitAccount();
                transmit.Argument.ActionName = actionName;
                transmit.Argument.SenderAccount = account;
                transmit.Argument.SenderClientId = clientId;
                transmit.Argument.TargetAccounts.AddAll(group.Accounts);
                if (null != parameter)
                {
                    transmit.Argument.Parameter = parameter;
                }

                if (false == ProviderApp.ProviderDirectService.ProviderByServerId.TryGetValue(group.ServerId, out var ps))
                {
                    groupLocal.AddAll(group.Accounts);
                    continue;
                }
                var socket = ProviderApp.ProviderDirectService.GetSocket(ps.SessionId);
                if (null == socket)
                {
                    groupLocal.AddAll(group.Accounts);
                    continue;
                }
                transmit.Send(socket);
            }
            if (groupLocal.Accounts.Count > 0)
                ProcessTransmit(account, clientId, actionName, groupLocal.Accounts, parameter);
        }

        public void Transmit(string account, string clientId, string actionName, ICollection<string> targets, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);

            var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.Encode(parameter));
            // 发送协议请求在另外的事务中执行。
            _ = ProviderApp.Zeze.NewProcedure(async () =>
            {
                await TransmitInProcedure(account, clientId, actionName, targets, binaryParam);
                return ResultCode.Success;
            }, "Onlines.Transmit").CallAsync();
        }

        public void TransmitWhileCommit(string account, string clientId, string actionName, string target, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(account, clientId, actionName, target, parameter));
        }

        public void TransmitWhileCommit(string account, string clientId, string actionName, ICollection<string> targets, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileCommit(() => Transmit(account, clientId, actionName, targets, parameter));
        }

        public void TransmitWhileRollback(string account, string clientId, string actionName, string target, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(account, clientId, actionName, target, parameter));
        }

        public void TransmitWhileRollback(string account, string clientId, string actionName, ICollection<string> targets, Serializable parameter = null)
        {
            if (false == TransmitActions.ContainsKey(actionName))
                throw new Exception("Unknown Action Name: " + actionName);
            Transaction.Transaction.Current.RunWhileRollback(() => Transmit(account, clientId, actionName, targets, parameter));
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
            string account = null;
            _tLocal.WalkCache(
                (k, v) =>
                {
                    // 先得到roleId
                    account = k;
                    return true;
                },
                () =>
                {
                    // 锁外执行事务
                    try
                    {
                        ProviderApp.Zeze.NewProcedure(async () =>
                        {
                            await TryRemoveLocal(account);
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

        private async Task TryRemoveLocal(string account)
        {
            var online = await _tOnline.GetAsync(account);
            var local = await _tLocal.GetAsync(account);
            var version = await _tVersion.GetOrAddAsync(account);
            if (null == local)
                return;
            // null == online && null == local -> do nothing
            // null != online && null == local -> do nothing

            if (null == online)
            {
                // remove all
                foreach (var loginLocal in local.Logins)
                    await RemoveLocalAndTrigger(account, loginLocal.Key);
            }
            else
            {
                // 在全局数据中查找login-local，删除不存在或者版本不匹配的。
                foreach (var loginLocal in local.Logins)
                {
                    if (false == version.Logins.TryGetValue(loginLocal.Key, out var loginVersion)
                        || loginVersion.LoginVersion != loginLocal.Value.LoginVersion)
                    {
                        await RemoveLocalAndTrigger(account, loginLocal.Key);
                    }
                }
            }

        }

        [RedirectToServer]
        protected virtual async Task RedirectNotify(int serverId, string account)
        {
            await TryRemoveLocal(account);
        }

        private async Task TryRedirectNotify(int serverId, string account)
        {
            if (ProviderApp.Zeze.Config.ServerId != serverId
                && ProviderApp.ProviderDirectService.ProviderByServerId.ContainsKey(serverId))
            {
                await RedirectNotify(serverId, account);
            }
        }

        protected override async Task<long> ProcessLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as Login;
            var session = ProviderUserSession.Get(rpc);

            var online = await _tOnline.GetOrAddAsync(session.Account);
            var local = await _tLocal.GetOrAddAsync(session.Account);
            var version = await _tVersion.GetOrAddAsync(session.Account);

            var loginLocal = local.Logins.GetOrAdd(rpc.Argument.ClientId);
            var loginVersion = version.Logins.GetOrAdd(rpc.Argument.ClientId);

            if (loginVersion.LoginVersion != loginVersion.LogoutVersion)
            {
                // login exist
                loginVersion.LogoutVersion = loginVersion.LoginVersion;
                await LogoutTriggerExtra(session.Account, rpc.Argument.ClientId);
            }
            if (loginVersion.LoginVersion != loginLocal.LoginVersion)
            {
                _ = TryRedirectNotify(loginVersion.ServerId, session.Account);
            }
            var loginVersionSerialId = version.LastLoginVersion + 1;
            version.LastLoginVersion = loginVersionSerialId;
            loginVersion.LoginVersion = loginVersionSerialId;
            loginLocal.LoginVersion = loginVersionSerialId;

            var loginOnline = online.Logins.GetOrAdd(rpc.Argument.ClientId);
            if (!loginOnline.LinkName.Equals(session.LinkName) || loginOnline.LinkSid != session.LinkSid)
            {
                ProviderApp.ProviderService.Kick(loginOnline.LinkName, loginOnline.LinkSid,
                        BKick.ErrorDuplicateLogin, $"duplicate login {session.Account}:{rpc.Argument.ClientId}");
            }

            /////////////////////////////////////////////////////////////
            // 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
            // 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
            // see Linkd.StableLinkSid
            if (false == loginOnline.LinkName.Equals(session.LinkName))
                loginOnline.LinkName = session.LinkName;
            if (loginOnline.LinkSid != session.LinkSid)
                loginOnline.LinkSid = session.LinkSid;
            /////////////////////////////////////////////////////////////

            loginVersion.ReliableNotifyConfirmIndex = 0;
            loginVersion.ReliableNotifyIndex = 0;
            loginVersion.ReliableNotifyMark.Clear();
            await OpenQueue(session.Account, rpc.Argument.ClientId).ClearAsync();

            var linkSession = session.Link.UserState as ProviderService.LinkSession;
            loginVersion.ServerId = ProviderApp.Zeze.Config.ServerId;

            await LoginTrigger(session.Account, rpc.Argument.ClientId);

            // 先提交结果再设置状态。
            // see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                setUserState.Argument.Context = rpc.Argument.ClientId;
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });
            //App.Load.LoginCount.IncrementAndGet();
            return ResultCode.Success;
        }

        protected override async Task<long> ProcessReLoginRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReLogin;
            var session = ProviderUserSession.Get(rpc);

            var online = await _tOnline.GetAsync(session.Account);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            var local = await _tLocal.GetOrAddAsync(session.Account);
            var version = await _tVersion.GetOrAddAsync(session.Account);

            var loginLocal = local.Logins.GetOrAdd(rpc.Argument.ClientId);
            var loginVersion = version.Logins.GetOrAdd(rpc.Argument.ClientId);

            /*
            if (loginVersion.LoginVersion != loginVersion.LogoutVersion)
            {
                // login exist
                // relogin 不需要补充 Logout？
                // loginVersion.LogoutVersion = loginVersion.LoginVersion;
                // await LogoutTriggerExtra(session.Account, rpc.Argument.ClientId);
            }
            */
            if (loginVersion.LoginVersion != loginLocal.LoginVersion)
            {
                _ = TryRedirectNotify(loginVersion.ServerId, session.Account);
            }
            var loginVersionSerialId = version.LastLoginVersion + 1;
            version.LastLoginVersion = loginVersionSerialId;
            loginVersion.LoginVersion = loginVersionSerialId;
            loginLocal.LoginVersion = loginVersionSerialId;

            /////////////////////////////////////////////////////////////
            // 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
            // 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
            // see Linkd.StableLinkSid
            var loginOnline = online.Logins.GetOrAdd(rpc.Argument.ClientId);
            if (false == loginOnline.LinkName.Equals(session.LinkName))
                loginOnline.LinkName = session.LinkName;
            if (loginOnline.LinkSid != session.LinkSid)
                loginOnline.LinkSid = session.LinkSid;
            /////////////////////////////////////////////////////////////

            await ReloginTrigger(session.Account, rpc.Argument.ClientId);

            // 先发结果，再发送同步数据（ReliableNotifySync）。
            // 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
            session.SendResponseWhileCommit(rpc);
            Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.LinkSid;
                setUserState.Argument.Context = rpc.Argument.ClientId;
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });

            var syncResultCode = await ReliableNotifySync(session.Account, rpc.Argument.ClientId,
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

            if (string.IsNullOrEmpty(session.Context))
                return ErrorCode(ResultCodeNotLogin);

            var local = await _tLocal.GetAsync(session.Account);
            var online = await _tOnline.GetAsync(session.Account);
            var version = await _tVersion.GetOrAddAsync(session.Account);

            var clientId = session.Context;
            var loginVersion = version.Logins.GetOrAdd(clientId);
            // 登录在其他机器上。
            if (local == null && online != null)
            {
                _ = TryRedirectNotify(loginVersion.ServerId, session.Account); // nowait
            }
            if (null != local)
                await RemoveLocalAndTrigger(session.Account, clientId);
            if (null != online)
            {
                loginVersion.LogoutVersion = loginVersion.LoginVersion;
                await LogoutTrigger(session.Account, clientId);
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

        private async Task<int> ReliableNotifySync(string account, string clientId,
            ProviderUserSession session, long index, bool sync = true)
        {
            var online = await _tVersion.GetOrAddAsync(account);
            var queue = OpenQueue(account, clientId);
            var loginOnline = online.Logins.GetOrAdd(clientId);
            if (index < loginOnline.ReliableNotifyConfirmIndex
                || index > loginOnline.ReliableNotifyIndex
                || index - loginOnline.ReliableNotifyConfirmIndex > await queue.CountAsync())
            {
                return ResultCodeReliableNotifyConfirmIndexOutOfRange;
            }

            int confirmCount = (int)(index - loginOnline.ReliableNotifyConfirmIndex);
            for (int i = 0; i < confirmCount; i++)
                await queue.PollAsync();
            loginOnline.ReliableNotifyConfirmIndex = index;

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
            return ResultCodeSuccess;
        }

        protected override async Task<long> ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as ReliableNotifyConfirm;
            var session = ProviderUserSession.Get(rpc);

            var clientId = session.Context;
            var online = await _tOnline.GetAsync(session.Account);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            session.SendResponseWhileCommit(rpc); // 同步前提交。
            var syncResultCode = await ReliableNotifySync(session.Account, clientId,
                session, rpc.Argument.ReliableNotifyConfirmIndex, rpc.Argument.Sync);

            if (ResultCodeSuccess != syncResultCode)
                return ErrorCode((ushort)syncResultCode);

            return ResultCode.Success;
        }
    }
}
