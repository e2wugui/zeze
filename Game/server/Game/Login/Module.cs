
namespace Game.Login
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(Game.App app)
        {
            Onlines = new Onlines(_tonline);
        }

        public void Stop(Game.App app)
        {
        }

        public Onlines Onlines { get; private set; }

        public override int ProcessCAuth(CAuth protocol)
        {
            SAuth result = new SAuth();
            result.Argument.Account = protocol.Argument.Account;

            BAccount account = _taccount.Get(protocol.Argument.Account);
            if (null == account || false ==  account.Token.Equals(protocol.Argument.Token))
            {
                result.Send(protocol.Sender);
                return Zeze.Transaction.Procedure.LogicError;
            }

            Game.App.Instance.Server.GetSocket(account.SocketSessionId)?.Dispose(); // kick, 最好发个协议再踢。如果允许多个连接，去掉这行。
            account.SocketSessionId = protocol.Sender.SessionId;

            result.ResultCode = SAuth.ResultSuccess;
            result.Send(protocol.Sender);

            // 验证成功. 把验证状态保存到连接中（客户端直连）。
            // Zeze框架会在合适的时候把 Sender.UserState 复制到 Protocol.UserState 中。
            // 为了方便修改成不是直连方式（连接Gate，通过Gate转发），其他协议处理的时候不要直接使用 Sender。
            protocol.Sender.UserState = new Session(protocol.Argument.Account, protocol.Sender);
            return Zeze.Transaction.Procedure.Success;
        }

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

            // initialize role data
            Game.App.Instance.Game_Bag_Module.GetBag(roleid).SetCapacity(50);

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
            foreach (var roleId in account.Roles)
            {
                BRoleData roleData = _trole.Get(roleId);
                if (null != roleData)
                    result.Argument.RoleList.Add(new BRole() { Id = roleId, Name = roleData.Name });
            }
            result.Argument.LastLoginRoleId = account.LastLoginRoleId;

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
                result.ResultCode = 1;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }
            account.LastLoginRoleId = protocol.Argument.RoleId;
            BRoleData role = _trole.Get(protocol.Argument.RoleId);
            if (null == role)
            {
                result.ResultCode = 2;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.LogicError;
            }

            BOnline online = _tonline.GetOrAdd(protocol.Argument.RoleId);
            online.RoleId = protocol.Argument.RoleId; // 对于同一个 session，允许重复登录 role，直接覆盖
            online.SessionId = session.SessionId;
            session.LoginRoleId = protocol.Argument.RoleId;
            session.SendResponse(result);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCLogout(CLogout protocol)
        {
            Session session = Session.Get(protocol);
            session.LoginRoleId = null;
            _tonline.Remove(protocol.Argument.RoleId);
            SLogout result = new SLogout();
            result.Argument.RoleId = protocol.Argument.RoleId;
            session.SendResponse(result);
            return Zeze.Transaction.Procedure.Success;
        }
    }
}
