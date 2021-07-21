
using System.Collections.Generic;
using Zeze.Transaction;

namespace Game.Login
{
    public sealed partial class ModuleLogin : AbstractModule
    {
        public void Start(Game.App app)
        {
            Onlines = new Onlines(_tonline);
        }

        public void Stop(Game.App app)
        {
        }

        public Onlines Onlines { get; private set; }

        public override int ProcessCreateRoleRequest(CreateRole rpc)
        {
            Session session = Session.Get(rpc);

            long roleid = _trole.Insert(new BRoleData()
            {
                Name = rpc.Argument.Name
            });

            // duplicate name check
            if (false == _trolename.TryAdd(rpc.Argument.Name, new BRoleId() { Id = roleid }))
                return ReturnCode(ResultCodeCreateRoleDuplicateRoleName);

            var account = _taccount.GetOrAdd(session.Account);
            account.Roles.Add(roleid);

            // initialize role data
            Game.App.Instance.Game_Bag.GetBag(roleid).SetCapacity(50);

            session.SendResponse(rpc);
            return Procedure.Success;
        }

        public override int ProcessGetRoleListRequest(GetRoleList rpc)
        {
            Session session = Session.Get(rpc);

            BAccount account = _taccount.Get(session.Account);
            if (null != account)
            {
                foreach (var roleId in account.Roles)
                {
                    BRoleData roleData = _trole.Get(roleId);
                    if (null != roleData)
                    {
                        rpc.Result.RoleList.Add(new BRole()
                        {
                            Id = roleId,
                            Name = roleData.Name
                        });
                    }
                }
                rpc.Result.LastLoginRoleId = account.LastLoginRoleId;
            }

            session.SendResponse(rpc);
            return Procedure.Success;
        }

        public override int ProcessLoginRequest(Login rpc)
        {
            Session session = Session.Get(rpc);

            BAccount account = _taccount.Get(session.Account);
            if (null == account)
                return ReturnCode(ResultCodeAccountNotExist);

            account.LastLoginRoleId = rpc.Argument.RoleId;
            BRoleData role = _trole.Get(rpc.Argument.RoleId);
            if (null == role)
                return ReturnCode(ResultCodeRoleNotExist);

            BOnline online = _tonline.GetOrAdd(rpc.Argument.RoleId);
            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            online.ReliableNotifyConfirmCount = 0;
            online.ReliableNotifyTotalCount = 0;
            online.ReliableNotifyMark.Clear();
            online.ReliableNotifyQueue.Clear();

            var linkSession = session.Link.UserState as Game.Server.LinkSession;
            online.ProviderId = App.Zeze.Config.AutoKeyLocalId;
            online.ProviderSessionId = linkSession.ProviderSessionId;

            // 先提交结果再设置状态。
            // see linkd::gnet.Provider.ModuleProvider。ProcessBroadcast
            session.SendResponseWhileCommit(rpc); 
            Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new gnet.Provider.SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(rpc.Argument.RoleId);
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });
            App.Load.LoginCount.IncrementAndGet();
            return Procedure.Success;
        }

        public override int ProcessReLoginRequest(ReLogin rpc)
        {
            Session session = Session.Get(rpc);

            BAccount account = _taccount.Get(session.Account);
            if (null == account)
                return ReturnCode(ResultCodeAccountNotExist);

            if (account.LastLoginRoleId != rpc.Argument.RoleId)
                return ReturnCode(ResultCodeNotLastLoginRoleId);

            BRoleData role = _trole.Get(rpc.Argument.RoleId);
            if (null == role)
                return ReturnCode(ResultCodeRoleNotExist);

            BOnline online = _tonline.Get(rpc.Argument.RoleId);
            if (null == online)
                return ReturnCode(ResultCodeOnlineDataNotFound);

            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            // 先发结果，再发送同步数据（ReliableNotifySync）。
            // 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
            session.SendResponseWhileCommit(rpc);
            Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new gnet.Provider.SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(rpc.Argument.RoleId);
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });

            var syncResultCode = ReliableNotifySync(
                session, rpc.Argument.ReliableNotifyConfirmCount,
                online);

            if (syncResultCode != ResultCodeSuccess)
                return ReturnCode((ushort)syncResultCode);

            App.Load.LoginCount.IncrementAndGet();
            return Procedure.Success;
        }

        private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online, bool sync = true)
        {
            if (ReliableNotifyConfirmCount < online.ReliableNotifyConfirmCount
                || ReliableNotifyConfirmCount > online.ReliableNotifyTotalCount
                || ReliableNotifyConfirmCount - online.ReliableNotifyConfirmCount > online.ReliableNotifyQueue.Count)
            {
                return ResultCodeReliableNotifyConfirmCountOutOfRange;
            }

            int confirmCount = (int)(ReliableNotifyConfirmCount - online.ReliableNotifyConfirmCount);

            if (sync)
            {
                var notify = new SReliableNotify();
                notify.Argument.ReliableNotifyTotalCountStart = ReliableNotifyConfirmCount;
                for (int i = confirmCount; i < online.ReliableNotifyQueue.Count; ++i)
                    notify.Argument.Notifies.Add(online.ReliableNotifyQueue[i]);
                session.SendResponseWhileCommit(notify);
            }
            online.ReliableNotifyQueue.RemoveRange(0, confirmCount);
            online.ReliableNotifyConfirmCount = ReliableNotifyConfirmCount;
            return ResultCodeSuccess;
        }

        public override int ProcessReliableNotifyConfirmRequest(ReliableNotifyConfirm rpc)
        {
            Session session = Session.Get(rpc);

            BOnline online = _tonline.Get(session.RoleId.Value);
            if (null == online || online.State == BOnline.StateOffline)
                return ReturnCode(ResultCodeOnlineDataNotFound);

            session.SendResponseWhileCommit(rpc); // 同步前提交。
            var syncResultCode = ReliableNotifySync(
                session,
                rpc.Argument.ReliableNotifyConfirmCount,
                online,
                false);

            if (ResultCodeSuccess != syncResultCode)
                return ReturnCode((ushort)syncResultCode);

            return Procedure.Success;
        }

        public override int ProcessLogoutRequest(Logout rpc)
        {
            Session session = Session.Get(rpc);

            if (session.RoleId == null)
                return ReturnCode(ResultCodeNotLogin);

            _tonline.Remove(session.RoleId.Value);

            // 先设置状态，再发送Logout结果。
            Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new gnet.Provider.SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });
            session.SendResponseWhileCommit(rpc);
            // 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
            // App.Load.LogoutCount.IncrementAndGet();
            return Procedure.Success;
        }
    }
}
