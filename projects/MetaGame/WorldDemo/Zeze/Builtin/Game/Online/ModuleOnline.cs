

using Zeze.Util;

namespace Zeze.Builtin.Game.Online
{
    public partial class ModuleOnline : AbstractModule
    {
        public void Start(global::Zeze.App app)
        {
        }

        public void Stop(global::Zeze.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessSReliableNotify(Zeze.Net.Protocol _p)
        {
            var p = _p as SReliableNotify;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
