
using Zeze.Util;

namespace Zeze.Builtin.Online
{
    public partial class ModuleOnline : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessSReliableNotify(Zeze.Net.Protocol _p)
        {
            var p = _p as SReliableNotify;
            return ResultCode.NotImplement;
        }

        public async Task LoginAsync(string clientId)
        {
            var p = new Login();
            p.Argument.ClientId = clientId;
            await p.SendAsync(App.Connector.TryGetReadySocket());
            if (p.ResultCode != 0)
                throw new Exception($"Login Error! Module={GetModuleId(p.ResultCode)} Code={GetErrorCode(p.ResultCode)}");
        }
    }
}
