
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Beans.Provider;
using Zeze.Net;
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

        protected override async Task<long> ProcessCreateRoleRequest(Protocol p)
        {
            var rpc = p as CreateRole;
            Session session = Session.Get(rpc);

            /*
             【警告】这里使用了AutoKey，这个是用来给游戏分服运营方式生成服务器之间唯一Id用的。方便未来合服用的。
             如果你的项目没有分服合服这种操作，不建议使用。
             */
            long roleid = await _trole.InsertAsync(new BRoleData()
            {
                Name = rpc.Argument.Name
            });

            // duplicate name check
            if (false == await _trolename.TryAddAsync(rpc.Argument.Name, new BRoleId() { Id = roleid }))
                return ErrorCode(ResultCodeCreateRoleDuplicateRoleName);

            var account = await _taccount.GetOrAddAsync(session.Account);
            account.Roles.Add(roleid);

            // initialize role data
            (await Game.App.Instance.Game_Bag.GetBag(roleid)).SetCapacity(50);

            session.SendResponse(rpc);
            return Procedure.Success;
        }

        protected override async Task<long> ProcessGetRoleListRequest(Protocol p)
        {
            var rpc = p as GetRoleList;
            Session session = Session.Get(rpc);

            BAccount account = await _taccount.GetAsync(session.Account);
            if (null != account)
            {
                foreach (var roleId in account.Roles)
                {
                    BRoleData roleData = await _trole.GetAsync(roleId);
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

        protected override async Task<long> ProcessLoginRequest(Protocol p)
        {
            var rpc = p as Login;
            Session session = Session.Get(rpc);

            BAccount account = await _taccount.GetAsync(session.Account);
            if (null == account)
                return ErrorCode(ResultCodeAccountNotExist);

            account.LastLoginRoleId = rpc.Argument.RoleId;
            BRoleData role = await _trole.GetAsync(rpc.Argument.RoleId);
            if (null == role)
                return ErrorCode(ResultCodeRoleNotExist);

            BOnline online = await _tonline.GetOrAddAsync(rpc.Argument.RoleId);
            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            online.ReliableNotifyConfirmCount = 0;
            online.ReliableNotifyTotalCount = 0;
            online.ReliableNotifyMark.Clear();
            online.ReliableNotifyQueue.Clear();

            var linkSession = session.Link.UserState as Game.Server.LinkSession;
            online.ProviderId = App.Zz.Config.ServerId;
            online.ProviderSessionId = linkSession.ProviderSessionId;

            // 先提交结果再设置状态。
            // see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
            session.SendResponseWhileCommit(rpc); 
            Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(rpc.Argument.RoleId);
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });
            App.Load.LoginCount.IncrementAndGet();
            return Procedure.Success;
        }

        protected override async Task<long> ProcessReLoginRequest(Protocol p)
        {
            var rpc = p as ReLogin;
            Session session = Session.Get(rpc);

            BAccount account = await _taccount.GetAsync(session.Account);
            if (null == account)
                return ErrorCode(ResultCodeAccountNotExist);

            if (account.LastLoginRoleId != rpc.Argument.RoleId)
                return ErrorCode(ResultCodeNotLastLoginRoleId);

            BRoleData role = await _trole.GetAsync(rpc.Argument.RoleId);
            if (null == role)
                return ErrorCode(ResultCodeRoleNotExist);

            BOnline online = await _tonline.GetAsync(rpc.Argument.RoleId);
            if (null == online)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            // 先发结果，再发送同步数据（ReliableNotifySync）。
            // 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
            session.SendResponseWhileCommit(rpc);
            Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(rpc.Argument.RoleId);
                rpc.Sender.Send(setUserState); // 直接使用link连接。
            });

            var syncResultCode = ReliableNotifySync(
                session, rpc.Argument.ReliableNotifyConfirmCount,
                online);

            if (syncResultCode != ResultCodeSuccess)
                return ErrorCode((ushort)syncResultCode);

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

        protected override async Task<long> ProcessReliableNotifyConfirmRequest(Protocol p)
        {
            var rpc = p as ReliableNotifyConfirm;
            Session session = Session.Get(rpc);

            BOnline online = await _tonline.GetAsync(session.RoleId.Value);
            if (null == online || online.State == BOnline.StateOffline)
                return ErrorCode(ResultCodeOnlineDataNotFound);

            session.SendResponseWhileCommit(rpc); // 同步前提交。
            var syncResultCode = ReliableNotifySync(
                session,
                rpc.Argument.ReliableNotifyConfirmCount,
                online,
                false);

            if (ResultCodeSuccess != syncResultCode)
                return ErrorCode((ushort)syncResultCode);

            return Procedure.Success;
        }

        protected override async Task<long> ProcessLogoutRequest(Protocol p)
        {
            var rpc = p as Logout;
            Session session = Session.Get(rpc);

            if (session.RoleId == null)
                return ErrorCode(ResultCodeNotLogin);

            await _tonline.RemoveAsync(session.RoleId.Value);

            // 先设置状态，再发送Logout结果。
            Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new SetUserState();
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
