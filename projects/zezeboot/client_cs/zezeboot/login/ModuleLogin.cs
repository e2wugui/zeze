using System.Threading.Tasks;
using Zeze.Net;

namespace zezeboot.login
{
    public class ModuleLogin : AbstractModulelogin
    {
        public static readonly ModuleLogin Instance = new ModuleLogin();

        public void OnAuthed(AsyncSocket so)
        {
            var r = new GetRoleList().Send(so, rpc =>
            {
                var rc = rpc.ResultCode;
                var res = ((GetRoleList)rpc).Result;
                Log.Info($"Recv GetRoleList resp({rc}): {res}");
                if (rc == 0)
                {
                    if (res.RoleList.Count == 0)
                        CreateRole(rpc.Sender);
                    else
                        LoginRole(rpc.Sender, res.RoleList[0].RoleId);
                }
                return Task.FromResult(0L);
            });
            Log.Info($"Send GetRoleList: {r}");
        }

        private void CreateRole(AsyncSocket so)
        {
            var createRole = new CreateRole
            {
                Argument = new BCreateRole
                {
                    Name = "TestRoleNameCs"
                }
            };
            var r = createRole.Send(so, rpc =>
            {
                var rc = rpc.ResultCode;
                var res = ((CreateRole)rpc).Result;
                Log.Info($"recv CreateRole resp({rc}): {res}");
                if (rc == 0)
                    LoginRole(rpc.Sender, res.RoleId);
                return Task.FromResult(0L);
            });
            Log.Info($"Send CreateRole: {r}");
        }

        private void LoginRole(AsyncSocket so, long roleId)
        {
            var loginRole = new LoginRole
            {
                Argument = new BRoleId
                {
                    RoleId = roleId
                }
            };
            var r = loginRole.Send(so, rpc =>
            {
                var rc = rpc.ResultCode;
                Log.Info($"Recv LoginRole resp({rc})");
                if (rc == 0)
                {
                    var success = new HelloWorld().Send(rpc.Sender);
                    if (success)
                        Log.Info("Send HelloWorld = true");
                    else
                        Log.Error("Send HelloWorld = false");
                }
                return Task.FromResult(0L);
            });
            Log.Info($"Send LoginRole: {r}");
        }

        protected override Task<long> ProcessKick(Protocol p)
        {
            Log.Info("Recv Kick");
            LinkClient.Instance.Stop();
            return Task.FromResult(0L);
        }
    }
}
