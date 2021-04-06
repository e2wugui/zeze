
using System.Collections.Generic;

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

        public override int ProcessCCreateRole(CCreateRole protocol)
        {
            Session session = Session.Get(protocol);

            SCreateRole result = new SCreateRole();
            result.Argument.Name = protocol.Argument.Name;

            long roleid = _trole.Insert(new BRoleData()
            {
                Name = protocol.Argument.Name
            });

            // duplicate name check
            if (false == _trolename.TryAdd(protocol.Argument.Name, new BRoleId() { Id = roleid }))
            {
                result.ResultCode = SCreateRole.ResultFaild;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }

            var account = _taccount.GetOrAdd(session.Account);
            account.Roles.Add(roleid);

            // initialize role data
            Game.App.Instance.Game_Bag.GetBag(roleid).SetCapacity(50);

            result.Argument.Id = roleid;
            result.ResultCode = SCreateRole.ResultSuccess;
            session.SendResponse(result);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCGetRoleList(CGetRoleList protocol)
        {
            Session session = Session.Get(protocol);

            SGetRoleList result = new SGetRoleList();
            BAccount account = _taccount.Get(session.Account);
            if (null != account)
            {
                foreach (var roleId in account.Roles)
                {
                    BRoleData roleData = _trole.Get(roleId);
                    if (null != roleData)
                        result.Argument.RoleList.Add(new BRole() { Id = roleId, Name = roleData.Name });
                }
                result.Argument.LastLoginRoleId = account.LastLoginRoleId;
            }

            session.SendResponse(result);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCLogin(CLogin protocol)
        {
            Session session = Session.Get(protocol);

            SLogin result = new SLogin();
            result.Argument.RoleId = protocol.Argument.RoleId;

            BAccount account = _taccount.Get(session.Account);
            if (null == account)
            {
                result.ResultCode = BLogin.ResultCodeAccountNotExist;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            account.LastLoginRoleId = protocol.Argument.RoleId;
            BRoleData role = _trole.Get(protocol.Argument.RoleId);
            if (null == role)
            {
                result.ResultCode = BLogin.ResultCodeRoleNotExist;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }

            BOnline online = _tonline.GetOrAdd(protocol.Argument.RoleId);
            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            online.ReliableNotifyConfirmCount = 0;
            online.ReliableNotifyTotalCount = 0;
            online.ReliableNotifyMark.Clear();
            online.ReliableNotifyQueue.Clear();

            // 先提交结果再设置状态。see linkd::gnet.Provider.ModuleProvider。ProcessBroadcast
            session.SendResponseWhileCommit(result); 

            Zeze.Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new gnet.Provider.SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(protocol.Argument.RoleId);
                protocol.Sender.Send(setUserState); // 直接使用link连接。
            });
            App.Load.LoginCount.IncrementAndGet();
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCReLogin(CReLogin protocol)
        {
            Session session = Session.Get(protocol);

            SLogin result = new SLogin();
            result.Argument.RoleId = protocol.Argument.RoleId;

            BAccount account = _taccount.Get(session.Account);
            if (null == account)
            {
                result.ResultCode = BLogin.ResultCodeAccountNotExist;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            if (account.LastLoginRoleId != protocol.Argument.RoleId)
            {
                result.ResultCode = BLogin.ResultCodeNotLastLoginRoleId;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            BRoleData role = _trole.Get(protocol.Argument.RoleId);
            if (null == role)
            {
                result.ResultCode = BLogin.ResultCodeRoleNotExist;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }

            BOnline online = _tonline.Get(protocol.Argument.RoleId);
            if (null == online)
            {
                result.ResultCode = BLogin.ResultCodeOnlineDataNotFound;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }

            online.LinkName = session.LinkName;
            online.LinkSid = session.SessionId;
            online.State = BOnline.StateOnline;

            session.SendResponseWhileCommit(result); // 先发结果，再发送同步数据。
            Zeze.Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new gnet.Provider.SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                setUserState.Argument.States.Add(protocol.Argument.RoleId);
                protocol.Sender.Send(setUserState); // 直接使用link连接。
            });

            result.ResultCode = ReliableNotifySync(session, protocol.Argument.ReliableNotifyConfirmCount, online);
            if (BLogin.ResultCodeSuccess != result.ResultCode)
            {
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            App.Load.LoginCount.IncrementAndGet();
            return Zeze.Transaction.Procedure.Success;
        }

        private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online, bool sync = true)
        {
            if (ReliableNotifyConfirmCount < online.ReliableNotifyConfirmCount
                || ReliableNotifyConfirmCount > online.ReliableNotifyTotalCount
                || ReliableNotifyConfirmCount - online.ReliableNotifyConfirmCount > online.ReliableNotifyQueue.Count)
            {
                return BLogin.ResultCodeReliableNotifyConfirmCountOutOfRange;
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
            return BLogin.ResultCodeSuccess;
        }

        public override int ProcessCReliableNotifyConfirm(CReliableNotifyConfirm protocol)
        {
            Session session = Session.Get(protocol);

            var result = new SReliableNotifyConfirm();
            BOnline online = _tonline.Get(session.RoleId.Value);
            if (null == online || online.State == BOnline.StateOffline)
            {
                result.ResultCode = BLogin.ResultCodeOnlineDataNotFound;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            session.SendResponseWhileCommit(result); // 同步前提交。
            result.ResultCode = ReliableNotifySync(session, protocol.Argument.ReliableNotifyConfirmCount, online, false);
            if (BLogin.ResultCodeSuccess != result.ResultCode)
            {
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCLogout(CLogout protocol)
        {
            Session session = Session.Get(protocol);

            SLogout result = new SLogout();
            if (session.RoleId == null)
            {
                result.ResultCode = BLogin.ResultCodeNotLogin;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }

            _tonline.Remove(session.RoleId.Value);

            // 先设置状态，再发送CLogout结果。
            Zeze.Transaction.Transaction.Current.RunWhileCommit(() =>
            {
                var setUserState = new gnet.Provider.SetUserState();
                setUserState.Argument.LinkSid = session.SessionId;
                protocol.Sender.Send(setUserState); // 直接使用link连接。
            });
            result.ResultCode = BLogin.ResultCodeSuccess;
            session.SendResponseWhileCommit(result);
            // App.Load.LogoutCount.IncrementAndGet(); // 处理异常关闭，在离线（Offline）里面计数。
            return Zeze.Transaction.Procedure.Success;
        }
    }
}
