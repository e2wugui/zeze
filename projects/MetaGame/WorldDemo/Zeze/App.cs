
using Zeze.Builtin.Game.Online;
using Zeze.Builtin.World;
using Zeze.Net;
using Zeze.Serialize;

namespace Zeze
{
    public sealed partial class App
    {
        public Zeze.World.World World { get; private set; }
        public Connector Connector { get; private set; }

        public void Start()
        {
            CreateZeze(new Config());
            CreateService();

            World = new Zeze.World.World(Zeze, ClientService);

            CreateModules();
            Zeze.StartAsync().Wait(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。

            ClientService.Config.TryGetOrAddConnector("127.0.0.1", 12000, false, out var connector);
            StartService(); // 启动网络

            connector.GetReadySocket(); // Wait Ready.
            Connector = connector;
        }

        public async Task Test(string account, long roleId)
        {
            var auth = new Zezex.Linkd.Auth();
            auth.Argument.Account = account;
            await auth.SendAsync(Connector.Socket);

            var login = new Login();
            login.Argument.RoleId = roleId;
            await login.SendAsync(Connector.Socket);

            await World.SwitchWorld(1, new Vector3(), new Vector3());

            for (var i = 0; i < 100; ++i)
            {
                var move = new BMove();
                World.Map.SendCommand(BCommand.eMoveMmo, move);
                await Task.Delay(1000);
            }
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            StopModules(); // 关闭模块，卸载配置什么的。
            Zeze.Stop(); // 关闭数据库
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
