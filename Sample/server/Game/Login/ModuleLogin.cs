
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Game.Login
{
    public sealed partial class ModuleLogin : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        protected override async Task<long> ProcessCreateRoleRequest(Protocol p)
        {
            var rpc = p as CreateRole;
            var session = ProviderUserSession.Get(rpc);

            /*
             【警告】这里使用了AutoKey，这个是用来给游戏分服运营方式生成服务器之间唯一Id用的。方便未来合服用的。
             如果你的项目没有分服合服这种操作，不建议使用。
             */
            long roleid = await _tRole.InsertAsync(new BRoleData()
            {
                Name = rpc.Argument.Name
            });

            // duplicate name check
            if (false == await _tRolename.TryAddAsync(rpc.Argument.Name, new BRoleId() { Id = roleid }))
                return ErrorCode(ResultCodeCreateRoleDuplicateRoleName);

            var account = await _tAccount.GetOrAddAsync(session.Account);
            account.Roles.Add(roleid);

            // initialize role data
            (await Game.App.Instance.Game_Bag.GetBag(roleid)).SetCapacity(50);

            session.SendResponseWhileCommit(rpc);
            return ResultCode.Success;
        }

        protected override async Task<long> ProcessGetRoleListRequest(Protocol p)
        {
            var rpc = p as GetRoleList;
            var session = ProviderUserSession.Get(rpc);

            var account = await _tAccount.GetAsync(session.Account);
            if (null != account)
            {
                foreach (var roleId in account.Roles)
                {
                    BRoleData roleData = await _tRole.GetAsync(roleId);
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

            session.SendResponseWhileCommit(rpc);
            return ResultCode.Success;
        }
    }
}
