
namespace Zeze.Builtin.Game.Online
{
    public partial class ModuleOnline : AbstractModule
    {
        public void Start(global::ClientGame.App app)
        {
        }

        public void Stop(global::ClientGame.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessSReliableNotify(Zeze.Net.Protocol _p)
        {
            var p = _p as SReliableNotify;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
